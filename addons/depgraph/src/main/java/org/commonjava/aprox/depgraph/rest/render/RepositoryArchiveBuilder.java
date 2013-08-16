package org.commonjava.aprox.depgraph.rest.render;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectReferences;
import static org.commonjava.web.json.ser.ServletSerializerUtils.fromRequestBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.filer.PathUtils;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/archive" )
public class RepositoryArchiveBuilder
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private FileManager fileManager;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private CartoDataManager cartoManager;

    @Inject
    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private RequestAdvisor requestAdvisor;

    @POST
    @Path( "/zip" )
    @Produces( "application/zip" )
    public Response getZipRepository( @Context final HttpServletRequest req, @Context final HttpServletResponse resp )
    {
        final WebOperationConfigDTO dto = fromRequestBody( req, serializer, WebOperationConfigDTO.class );

        if ( dto == null )
        {
            logger.warn( "Repository archive configuration is missing." );
            return Response.status( Status.BAD_REQUEST )
                           .entity( "JSON configuration not supplied" )
                           .build();
        }

        if ( !dto.isValid() )
        {
            logger.warn( "Repository archive configuration is invalid: %s", dto );
            return Response.status( Status.BAD_REQUEST )
                           .entity( "Invalid configuration" )
                           .build();
        }

        logger.info( "Building repository for: %s", dto );

        final StoreKey key = dto.getSource();

        Response response = Response.status( NO_CONTENT )
                                    .build();

        ZipOutputStream stream = null;
        EProjectWeb web = null;

        boolean error = false;
        try
        {
            cartoManager.setCurrentWorkspace( dto.getWorkspaceId() );

            final ProjectRelationshipFilter presetFilter = requestAdvisor.getPresetFilter( dto.getPreset() );
            web = cartoManager.getProjectWeb( presetFilter, dto.getRoots()
                                                               .toArray( new ProjectVersionRef[dto.getRoots()
                                                                                                  .size()] ) );
            if ( web == null )
            {
                // TODO: discovery link
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }

            final Map<ProjectRef, ProjectRefCollection> refMap = collectProjectReferences( web );

            final OutputStream os = resp.getOutputStream();
            stream = new ZipOutputStream( os );

            final List<ArtifactStore> stores = getStoresFor( key );

            final Set<ArtifactRef> seen = new HashSet<>();
            for ( final ProjectRefCollection refs : refMap.values() )
            {
                for ( ArtifactRef ar : refs.getArtifactRefs() )
                {
                    logger.info( "Including: %s", ar );

                    if ( ar.isVariableVersion() )
                    {
                        final ProjectVersionRef specific =
                            discoverer.resolveSpecificVersion( ar, dto.getDiscoveryConfig() );
                        ar =
                            new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), specific.getVersionSpec(),
                                             ar.getType(), ar.getClassifier(), ar.isOptional() );
                        // resolve;
                    }

                    if ( !"pom".equals( ar.getType() ) )
                    {
                        final ArtifactRef pomAR =
                            new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(), "pom", null,
                                             false );

                        if ( !seen.contains( pomAR ) )
                        {
                            includeInZip( stream, pomAR, stores );
                            seen.add( pomAR );
                        }
                    }

                    if ( !seen.contains( ar ) )
                    {
                        includeInZip( stream, ar, stores );
                        seen.add( ar );
                    }
                }
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to generate runtime repository for: %s. Reason: %s", e, dto, e.getMessage() );

            response = Response.serverError()
                               .build();
            error = true;
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to generate runtime repository for: %s. Reason: %s", e, dto, e.getMessage() );

            response = Response.serverError()
                               .build();
            error = true;
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to lookup Artifact stores based on %s. Reason: %s", e, key, e.getMessage() );
            response = Response.serverError()
                               .build();
            error = true;
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to retrieve relevant artifacts for repository archive of %s. Reason: %s", e, key,
                          e.getMessage() );

            response = e.getResponse();
            if ( response == null )
            {
                response = Response.serverError()
                                   .build();
            }
            error = true;
        }
        catch ( final URISyntaxException e )
        {
            logger.error( "Failed to construct DiscoveryConfig from: %s. Reason: %s", e, key, e.getMessage() );
            response = Response.serverError()
                               .build();
            error = true;
        }
        finally
        {
            closeQuietly( stream );
        }

        if ( error )
        {
            return response;
        }

        boolean discoveryEligible = false;
        if ( web.getIncompleteSubgraphs() != null )
        {
            // TODO: missing-resolution link
            discoveryEligible = true;
        }

        if ( web.getVariableSubgraphs() != null )
        {
            // TODO: variable-resolution link
            discoveryEligible = true;
        }

        if ( discoveryEligible )
        {
            // TODO: discovery link
        }

        response = Response.ok()
                           .type( "application/zip" )
                           .build();

        return response;
    }

    private boolean includeInZip( final ZipOutputStream stream, final ArtifactRef ar, final List<ArtifactStore> stores )
        throws IOException, AproxWorkflowException
    {
        final String version = ar.getVersionString();

        final StringBuilder sb = new StringBuilder();
        sb.append( ar.getArtifactId() )
          .append( '-' )
          .append( version );
        if ( ar.getClassifier() != null )
        {
            sb.append( '-' )
              .append( ar.getClassifier() );
        }

        sb.append( '.' )
          .append( ar.getType() );

        final String path = PathUtils.join( ar.getGroupId()
                                              .replace( '.', '/' ), ar.getArtifactId(), version, sb.toString() );
        final ZipEntry ze = new ZipEntry( path );
        stream.putNextEntry( ze );

        logger.info( "Attempting to resolve: %s from: %s", path, stores );
        final Transfer item = fileManager.retrieveFirst( stores, path );
        logger.info( "Got: %s", item );

        if ( item == null )
        {
            logger.warn( "NOT FOUND: %s", ar );
            return false;
        }

        InputStream itemStream = null;
        try
        {
            itemStream = item.openInputStream();
            copy( itemStream, stream );
        }
        finally
        {
            closeQuietly( itemStream );
        }

        return true;
    }

    private List<ArtifactStore> getStoresFor( final StoreKey key )
        throws ProxyDataException
    {
        final StoreType st = key.getType();
        List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        if ( st == StoreType.group )
        {
            stores = storeManager.getOrderedConcreteStoresInGroup( key.getName() );
        }
        else
        {
            stores.add( storeManager.getArtifactStore( key ) );
        }

        return stores;
    }

}
