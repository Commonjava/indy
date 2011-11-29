/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.rest.access;

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
