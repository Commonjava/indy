package org.commonjava.web.maven.proxy.rest.access;

import java.io.File;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.auth.couch.model.Permission;
import org.commonjava.web.maven.proxy.model.ArtifactStore;
import org.commonjava.web.maven.proxy.rest.util.FileManager;

public abstract class AbstractSimpleAccessResource<T extends ArtifactStore>
{

    @Inject
    private FileManager fileManager;

    protected AbstractSimpleAccessResource()
    {}

    @GET
    @Path( "/{name}{path: (/.+)?}" )
    public Response getContent( @PathParam( "name" ) final String name,
                                @PathParam( "path" ) final String path )
    {
        checkPermission( name, Permission.READ );

        // TODO:
        // 1. directory request (ends with "/")...browse somehow??
        // 2. empty path (directory request for proxy root)

        ArtifactStore store = getArtifactStore( name );

        if ( store == null )
        {
            throw new WebApplicationException(
                                               Response.status( Status.NOT_FOUND ).entity( "Artifact store not found: "
                                                                                               + name ).build() );
        }

        File target = fileManager.download( store, path );

        String mimeType = new MimetypesFileTypeMap().getContentType( target );
        return Response.ok( target, mimeType ).build();
    }

    protected abstract void checkPermission( String name, String access );

    protected abstract T getArtifactStore( String name );

    protected FileManager getFileManager()
    {
        return fileManager;
    }

}