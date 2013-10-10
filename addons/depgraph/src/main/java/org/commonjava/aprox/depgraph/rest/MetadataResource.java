package org.commonjava.aprox.depgraph.rest;

import static org.apache.commons.lang.StringUtils.join;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.depgraph.dto.MetadataCollationDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.MetadataCollation;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;
import org.commonjava.web.json.ser.ServletSerializerUtils;

import com.google.gson.reflect.TypeToken;

@Path( "/depgraph/meta" )
@Consumes( MediaType.APPLICATION_JSON )
@Produces( MediaType.APPLICATION_JSON )
@RequestScoped
public class MetadataResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private MetadataOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private LocationExpander locationExpander;

    @Path( "/batch" )
    @POST
    public Response batchUpdate( @Context final HttpServletRequest request )
    {
        Response response = Response.status( Status.NOT_MODIFIED )
                                    .build();

        final TypeToken<Map<ProjectVersionRef, Map<String, String>>> tt = new TypeToken<Map<ProjectVersionRef, Map<String, String>>>()
        {
        };

        final Map<ProjectVersionRef, Map<String, String>> batch = ServletSerializerUtils.fromRequestBody( request, serializer, tt );

        if ( batch != null && !batch.isEmpty() )
        {
            for ( final Map.Entry<ProjectVersionRef, Map<String, String>> entry : batch.entrySet() )
            {
                final ProjectVersionRef ref = entry.getKey();
                final Map<String, String> metadata = entry.getValue();

                logger.info( "Adding metadata for: %s\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );
                ops.updateMetadata( ref, metadata );
            }

            response = Response.ok()
                               .build();
        }

        return response;
    }

    @Path( "/for/{g}/{a}/{v}" )
    @GET
    public Response getMetadata( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                 @PathParam( "v" ) final String version )
    {
        Response response = Response.status( Status.NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        Map<String, String> metadata = null;
        try
        {
            metadata = ops.getMetadata( ref );
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve metadata map for: %s. Reason: %s", e, ref, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        if ( metadata != null )
        {
            final String json = serializer.toString( metadata );
            response = Response.ok( json )
                               .build();
        }

        return response;
    }

    @Path( "/forkey/{g}/{a}/{v}/{k}" )
    @GET
    public Response getMetadataValue( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                      @PathParam( "v" ) final String version, @PathParam( "k" ) final String key )
    {
        Response response = Response.status( Status.NO_CONTENT )
                                    .build();

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final String value = ops.getMetadataValue( ref, key );
            if ( value != null )
            {
                final String json = serializer.toString( Collections.singletonMap( key, value ) );
                response = Response.ok( json )
                                   .build();
            }
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve metadata map for: %s. Reason: %s", e, ref, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    @Path( "/{g}/{a}/{v}" )
    @POST
    public Response updateMetadata( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                                    @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        Response response = Response.status( Status.NOT_MODIFIED )
                                    .build();

        final TypeToken<Map<String, String>> tt = new TypeToken<Map<String, String>>()
        {
        };

        final Map<String, String> metadata = ServletSerializerUtils.fromRequestBody( request, serializer, tt );

        if ( metadata != null && !metadata.isEmpty() )
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

            logger.info( "Adding metadata for: %s\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );

            ops.updateMetadata( ref, metadata );

            response = Response.ok()
                               .build();
        }

        return response;
    }

    @Path( "/collate" )
    @POST
    public Response getCorrelation( @Context final HttpServletRequest req )
    {
        Response response = Response.status( Status.NOT_FOUND )
                                    .build();

        try
        {
            final MetadataCollationDTO dto = readCollationDTO( req );
            if ( dto == null )
            {
                return response;
            }

            try
            {
                final MetadataCollation result = ops.collate( dto );
                final String json = serializer.toString( result );
                response = Response.ok( json )
                                   .build();
            }
            catch ( final CartoDataException e )
            {
                throw new AproxWorkflowException( "Failed to resolve or collate graph contents by metadata: %s. Reason: %s", e, dto, e.getMessage() );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = e.getResponse();
        }

        return response;
    }

    private MetadataCollationDTO readCollationDTO( final HttpServletRequest req )
        throws AproxWorkflowException
    {
        String json;
        try
        {
            json = IOUtils.toString( req.getInputStream() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read configuration JSON from request body. Reason: %s", e, e.getMessage() );
        }

        logger.info( "Got configuration JSON:\n\n%s\n\n", json );
        final MetadataCollationDTO dto = serializer.fromString( json, MetadataCollationDTO.class );

        try
        {
            dto.calculateLocations( locationExpander );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( Status.BAD_REQUEST, "One or more sources/excluded sources is invalid: %s", e, e.getMessage() );
        }

        return dto;
    }

}
