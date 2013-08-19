package org.commonjava.aprox.depgraph.rest.render;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.cartographer.agg.AggregationUtils.collectProjectReferences;
import static org.commonjava.web.json.ser.ServletSerializerUtils.fromRequestBody;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.dto.ExtraCT;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.filer.PathUtils;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregationOptions;
import org.commonjava.maven.cartographer.agg.DefaultAggregatorOptions;
import org.commonjava.maven.cartographer.agg.ProjectRefCollection;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/downloads" )
public class DownloadManifestBuilder
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
    private ResolveOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private RequestAdvisor requestAdvisor;

    @POST
    @Path( "/urlmap" )
    @Produces( "application/json" )
    public Response getUrlMap( @Context final HttpServletRequest req, @Context final HttpServletResponse resp,
                               @Context final UriInfo info )
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

        final URI sourceUri = sourceManager.createSourceURI( dto.getSource()
                                                                .toString() );
        if ( sourceUri == null )
        {
            final String message =
                String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", dto.getSource(),
                               sourceManager.getFormatHint() );
            logger.warn( message );
            return Response.status( Status.BAD_REQUEST )
                           .entity( message )
                           .build();
        }

        logger.info( "Building repository for: %s", dto );

        final StoreKey key = dto.getSource();

        Response response = Response.status( NO_CONTENT )
                                    .build();

        EProjectWeb web = null;

        boolean error = false;
        final Map<ProjectVersionRef, Set<String>> urlMap = new HashMap<>();
        try
        {
            cartoManager.setCurrentWorkspace( dto.getWorkspaceId() );

            Collection<ProjectVersionRef> roots = dto.getRoots();
            ProjectVersionRef[] rootsArray = roots.toArray( new ProjectVersionRef[roots.size()] );

            final AggregationOptions options = createAggregationOptions( dto, sourceUri );
            if ( dto.isResolve() )
            {
                roots = ops.resolve( dto.getSource()
                                        .toString(), options, rootsArray );
                rootsArray = roots.toArray( new ProjectVersionRef[roots.size()] );
            }

            final ProjectRelationshipFilter presetFilter = requestAdvisor.getPresetFilter( dto.getPreset() );
            web = cartoManager.getProjectWeb( presetFilter, rootsArray );
            if ( web == null )
            {
                // TODO: discovery link
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }

            final Map<ProjectRef, ProjectRefCollection> refMap = collectProjectReferences( web );

            final List<ArtifactStore> stores = getStoresFor( key );

            final Set<ExtraCT> extras = dto.getExtras();
            final Set<ArtifactRef> seen = new HashSet<>();

            for ( final ProjectRefCollection refs : refMap.values() )
            {
                for ( ArtifactRef ar : refs.getArtifactRefs() )
                {
                    final Set<String> urls = new HashSet<>();
                    urlMap.put( ar.asProjectVersionRef(), urls );

                    logger.info( "Including: %s", ar );

                    if ( ar.isVariableVersion() )
                    {
                        final ProjectVersionRef specific =
                            discoverer.resolveSpecificVersion( ar, options.getDiscoveryConfig() );
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
                            final Transfer item = resolve( pomAR, stores );
                            if ( item != null )
                            {
                                urls.add( formatUrlMapUrl( item, info ) );
                            }
                            seen.add( pomAR );
                        }
                    }

                    if ( !seen.contains( ar ) )
                    {
                        final Transfer item = resolve( ar, stores );
                        if ( item != null )
                        {
                            urls.add( formatUrlMapUrl( item, info ) );
                        }
                        seen.add( ar );
                    }

                    if ( extras != null )
                    {
                        for ( final ExtraCT extraCT : extras )
                        {
                            final ArtifactRef extAR =
                                new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(),
                                                 extraCT.getType(), extraCT.getClassifier(), false );

                            if ( !seen.contains( extAR ) )
                            {
                                final Transfer item = resolve( extAR, stores );
                                if ( item != null )
                                {
                                    urls.add( formatUrlMapUrl( item, info ) );
                                }

                                seen.add( extAR );
                            }
                        }
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

        final String json = serializer.toString( urlMap );

        response = Response.ok( json )
                           .type( "application/json" )
                           .build();

        return response;
    }

    @POST
    @Path( "/downlog" )
    @Produces( "text/plain" )
    public Response getDownloadLog( @Context final HttpServletRequest req, @Context final HttpServletResponse resp,
                                    @Context final UriInfo info )
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

        final URI sourceUri = sourceManager.createSourceURI( dto.getSource()
                                                                .toString() );
        if ( sourceUri == null )
        {
            final String message =
                String.format( "Invalid source format: '%s'. Use the form: '%s' instead.", dto.getSource(),
                               sourceManager.getFormatHint() );
            logger.warn( message );
            return Response.status( Status.BAD_REQUEST )
                           .entity( message )
                           .build();
        }

        logger.info( "Building repository for: %s", dto );

        final StoreKey key = dto.getSource();

        Response response = Response.status( NO_CONTENT )
                                    .build();

        EProjectWeb web = null;

        boolean error = false;
        final Set<String> downLog = new HashSet<>();
        try
        {
            cartoManager.setCurrentWorkspace( dto.getWorkspaceId() );

            Collection<ProjectVersionRef> roots = dto.getRoots();
            ProjectVersionRef[] rootsArray = roots.toArray( new ProjectVersionRef[roots.size()] );

            final AggregationOptions options = createAggregationOptions( dto, sourceUri );
            if ( dto.isResolve() )
            {
                roots = ops.resolve( dto.getSource()
                                        .toString(), options, rootsArray );
                rootsArray = roots.toArray( new ProjectVersionRef[roots.size()] );
            }

            final ProjectRelationshipFilter presetFilter = requestAdvisor.getPresetFilter( dto.getPreset() );
            web = cartoManager.getProjectWeb( presetFilter, rootsArray );
            if ( web == null )
            {
                // TODO: discovery link
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }

            final Map<ProjectRef, ProjectRefCollection> refMap = collectProjectReferences( web );

            final List<ArtifactStore> stores = getStoresFor( key );

            final Set<ExtraCT> extras = dto.getExtras();
            final Set<ArtifactRef> seen = new HashSet<>();

            for ( final ProjectRefCollection refs : refMap.values() )
            {
                for ( ArtifactRef ar : refs.getArtifactRefs() )
                {
                    logger.info( "Including: %s", ar );

                    if ( ar.isVariableVersion() )
                    {
                        final ProjectVersionRef specific =
                            discoverer.resolveSpecificVersion( ar, options.getDiscoveryConfig() );
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
                            final Transfer item = resolve( pomAR, stores );
                            if ( item != null )
                            {
                                downLog.add( formatDownlogEntry( item, info ) );
                            }
                            seen.add( pomAR );
                        }
                    }

                    if ( !seen.contains( ar ) )
                    {
                        final Transfer item = resolve( ar, stores );
                        if ( item != null )
                        {
                            downLog.add( formatDownlogEntry( item, info ) );
                        }
                        seen.add( ar );
                    }

                    if ( extras != null )
                    {
                        for ( final ExtraCT extraCT : extras )
                        {
                            final ArtifactRef extAR =
                                new ArtifactRef( ar.getGroupId(), ar.getArtifactId(), ar.getVersionSpec(),
                                                 extraCT.getType(), extraCT.getClassifier(), false );

                            if ( !seen.contains( extAR ) )
                            {
                                final Transfer item = resolve( extAR, stores );
                                if ( item != null )
                                {
                                    downLog.add( formatDownlogEntry( item, info ) );
                                }

                                seen.add( extAR );
                            }
                        }
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

        final String output = join( downLog, "\n" );

        response = Response.ok( output )
                           .type( "application/json" )
                           .build();

        return response;
    }

    private String formatDownlogEntry( final Transfer item, final UriInfo info )
        throws MalformedURLException
    {
        final KeyedLocation kl = (KeyedLocation) item.getLocation();
        final StoreKey key = kl.getKey();

        final URI uri = info.getBaseUriBuilder()
                            .path( key.getType()
                                      .singularEndpointName() )
                            .path( key.getName() )
                            .path( item.getPath() )
                            .build();

        return String.format( "Downloading %s", uri.toURL()
                                                   .toExternalForm() );
    }

    private String formatUrlMapUrl( final Transfer item, final UriInfo info )
        throws MalformedURLException
    {
        final KeyedLocation kl = (KeyedLocation) item.getLocation();
        final StoreKey key = kl.getKey();

        final URI uri = info.getBaseUriBuilder()
                            .path( key.getType()
                                      .singularEndpointName() )
                            .path( key.getName() )
                            .path( item.getPath() )
                            .build();

        return uri.toURL()
                  .toExternalForm();
    }

    private AggregationOptions createAggregationOptions( final WebOperationConfigDTO dto, final URI sourceUri )
    {
        final DefaultAggregatorOptions options = new DefaultAggregatorOptions();
        options.setFilter( requestAdvisor.getPresetFilter( dto.getPreset() ) );

        final DefaultDiscoveryConfig dconf = new DefaultDiscoveryConfig( sourceUri );

        dconf.setEnabled( true );
        dconf.setTimeoutMillis( 1000 * dto.getTimeoutSecs() );

        options.setDiscoveryConfig( dconf );

        options.setProcessIncompleteSubgraphs( true );
        options.setProcessVariableSubgraphs( true );

        return options;
    }

    private Transfer resolve( final ArtifactRef ar, final List<ArtifactStore> stores )
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

        sb.append( '.' );
        if ( "maven-plugin".equals( ar.getType() ) )
        {
            sb.append( "jar" );
        }
        else
        {
            sb.append( ar.getType() );
        }

        final String path = PathUtils.join( ar.getGroupId()
                                              .replace( '.', '/' ), ar.getArtifactId(), version, sb.toString() );

        logger.info( "Attempting to resolve: %s from: %s", path, stores );
        final Transfer item = fileManager.retrieveFirst( stores, path );
        logger.info( "Got: %s", item );

        if ( item == null )
        {
            logger.warn( "NOT FOUND: %s", ar );
        }

        return item;
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
