package org.commonjava.aprox.core.rest.admin;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.servlet.ServletException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.commonjava.aprox.core.rest.AbstractURLAliasingResource;

@Path( "/admin" )
@RequestScoped
public class AdminDeprecationResource
    extends AbstractURLAliasingResource
{

    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Path( "/repository" )
    public void createRepository()
        throws ServletException, IOException
    {
        forward( "/../repositories" );
    }

    @POST
    @Path( "/repository/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void storeRepository( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../repositories", name );
    }

    @GET
    @Path( "/repository/list" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public void getAllRepositories()
        throws ServletException, IOException
    {
        forward( "/../../repositories" );
    }

    @GET
    @Path( "/repository/{name}" )
    public void getRepository( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../repositories", name );
    }

    @DELETE
    @Path( "/repository/{name}" )
    public void deleteRepository( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../repositories", name );
    }

    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Path( "/deploy" )
    public void createDeployPoint()
        throws ServletException, IOException
    {
        forward( "/../deploys" );
    }

    @POST
    @Path( "/deploy/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void storeDeployPoint( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../deploys", name );
    }

    @GET
    @Path( "/deploy/list" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public void getAllDeployPoints()
        throws ServletException, IOException
    {
        forward( "/../../deploys" );
    }

    @GET
    @Path( "/deploy/{name}" )
    public void getDeployPoint( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../deploys", name );
    }

    @DELETE
    @Path( "/deploy/{name}" )
    public void deleteDeployPoint( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../deploys", name );
    }

    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Path( "/group" )
    public void createGroup()
        throws ServletException, IOException
    {
        forward( "/../groups" );
    }

    @POST
    @Path( "/group/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void storeGroup( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../groups,", name );
    }

    @GET
    @Path( "/group/list" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public void getAllGroups()
        throws ServletException, IOException
    {
        forward( "/../../groups" );
    }

    @GET
    @Path( "/group/{name}" )
    public void getGroup( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../groups", name );
    }

    @DELETE
    @Path( "/group/{name}" )
    public void deleteGroup( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        forward( "/../../groups", name );
    }
}
