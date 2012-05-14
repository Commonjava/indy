package org.commonjava.aprox.core.rest.admin;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Singleton
public class AdminDeprecationResource
{
    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Path( "/admin/repository" )
    public void createRepository()
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/repositories" )
               .forward( request, response );
    }

    @POST
    @Path( "/admin/repository/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void storeRepository( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/repositories/" + name )
               .forward( request, response );
    }

    @GET
    @Path( "/admin/repository/list" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public void getAllRepositories()
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/repositories/list" )
               .forward( request, response );
    }

    @GET
    @Path( "/admin/repository/{name}" )
    public void getRepository( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/repositories/" + name )
               .forward( request, response );
    }

    @DELETE
    @Path( "/admin/repository/{name}" )
    public void deleteRepository( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/repositories/" + name )
               .forward( request, response );
    }

    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Path( "/admin/deploy" )
    public void createDeployPoint()
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/deploys" )
               .forward( request, response );
    }

    @POST
    @Path( "/admin/deploy/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void storeDeployPoint( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/deploys/" + name )
               .forward( request, response );
    }

    @GET
    @Path( "/admin/deploy/list" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public void getAllDeployPoints()
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/deploys/list" )
               .forward( request, response );
    }

    @GET
    @Path( "/admin/deploy/{name}" )
    public void getDeployPoint( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/deploys/" + name )
               .forward( request, response );
    }

    @DELETE
    @Path( "/admin/deploy/{name}" )
    public void deleteDeployPoint( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/deploys/" + name )
               .forward( request, response );
    }

    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Path( "/admin/group" )
    public void createGroup()
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/groups" )
               .forward( request, response );
    }

    @POST
    @Path( "/admin/group/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public void storeGroup( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/groups/" + name )
               .forward( request, response );
    }

    @GET
    @Path( "/admin/group/list" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public void getAllGroups()
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/groups/list" )
               .forward( request, response );
    }

    @GET
    @Path( "/admin/group/{name}" )
    public void getGroup( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/groups/" + name )
               .forward( request, response );
    }

    @DELETE
    @Path( "/admin/group/{name}" )
    public void deleteGroup( @PathParam( "name" ) final String name )
        throws ServletException, IOException
    {
        request.getRequestDispatcher( "/admin/groups/" + name )
               .forward( request, response );
    }
}
