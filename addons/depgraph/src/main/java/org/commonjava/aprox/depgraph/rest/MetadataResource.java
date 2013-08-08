package org.commonjava.aprox.depgraph.rest;

import static org.apache.commons.lang.StringUtils.join;

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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.MetadataOps;
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
    private JsonSerializer serializer;

    @Path( "/batch" )
    @POST
    public Response batchUpdate( @Context final HttpServletRequest request )
    {
        Response response = Response.status( Status.NOT_MODIFIED )
                                    .build();

        final TypeToken<Map<ProjectVersionRef, Map<String, String>>> tt =
            new TypeToken<Map<ProjectVersionRef, Map<String, String>>>()
            {
            };

        final Map<ProjectVersionRef, Map<String, String>> batch =
            ServletSerializerUtils.fromRequestBody( request, serializer, tt );

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

    @Path( "/{g}/{a}/{v}/all" )
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

    @Path( "/{g}/{a}/{v}/{k}" )
    @GET
    public Response getMetadataValue( @PathParam( "g" ) final String groupId,
                                      @PathParam( "a" ) final String artifactId,
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
}
