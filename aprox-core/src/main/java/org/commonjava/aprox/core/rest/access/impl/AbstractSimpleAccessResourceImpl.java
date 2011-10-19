package org.commonjava.aprox.core.rest.access.impl;

import java.io.File;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.rest.util.FileManager;

public abstract class AbstractSimpleAccessResourceImpl<T extends ArtifactStore>
{

    @Inject
    private FileManager fileManager;

    protected AbstractSimpleAccessResourceImpl()
    {}

    @GET
    @Path( "/{name}{path: (/.+)?}" )
    public Response getContent( @PathParam( "name" ) final String name,
                                @PathParam( "path" ) final String path )
    {
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
        if ( target == null )
        {
            return Response.status( Status.NOT_FOUND ).build();
        }

        String mimeType = new MimetypesFileTypeMap().getContentType( target );
        return Response.ok( target, mimeType ).build();
    }

    protected abstract T getArtifactStore( String name );

    protected FileManager getFileManager()
    {
        return fileManager;
    }

}