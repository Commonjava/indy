package org.commonjava.indy.filer.def.jaxrs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.REST;
import org.commonjava.indy.filer.def.DefaultGalleyStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Set;

@Api( value = "Proxy Site Cache Access and Storage" )
@Path( "/api/proxysite" )
@ApplicationScoped
@REST
public class ProxySiteAccessResource
        implements IndyResources
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    DefaultGalleyStorageProvider provider;

    @ApiOperation( "Retrieve All Proxy Sites." )
    @ApiResponses( { @ApiResponse( code = 404, message = "Site is not available" ),
            @ApiResponse( code = 200, response = StreamingOutput.class, message = "Site stream" ), } )
    @Produces( "application/json" )
    @Path( "/all" )
    @GET
    public Response doGet( @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        Set<String> cache = provider.getProxySitesCache();
        logger.info( "Proxy Site Cache list: {}", cache );
        List<String> result = provider.getProxySiteList();
        return ( result == null || result.isEmpty() ) ?
                Response.status( Response.Status.NOT_FOUND ).build() :
                Response.ok( result ).build();
    }

    @ApiOperation( "Store Proxy Site." )
    @ApiResponses( { @ApiResponse( code = 201, message = "Site was stored successfully" ) } )
    @PUT
    @Path( "/{site}" )
    public Response doCreate( @PathParam( "site" ) final String site, @Context final HttpServletRequest request,
                              @Context final UriInfo uriInfo )
    {
        provider.saveProxySite( site );
        return Response.created( uriInfo.getRequestUri() ).build();
    }

    @ApiOperation( "Delete Proxy Site." )
    @ApiResponse( code = 200, message = "Delete complete." )
    @Path( "/{site}" )
    @DELETE
    public Response doDelete( @PathParam( "site" ) final String site )
    {
        provider.deleteProxySite( site );
        provider.getProxySitesCache().remove( site );
        return Response.ok().build();
    }

    @ApiOperation( "Delete All Proxy Sites." )
    @ApiResponse( code = 200, message = "Delete complete." )
    @Path( "/all" )
    @DELETE
    public Response doDeleteAll()
    {
        provider.deleteAllProxySite();
        provider.getProxySitesCache().clear();
        return Response.ok().build();
    }

}
