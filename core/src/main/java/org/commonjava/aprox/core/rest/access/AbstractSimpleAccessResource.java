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

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.core.io.StorageItem;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.rest.RESTWorkflowException;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.util.logging.Logger;

public abstract class AbstractSimpleAccessResource<T extends ArtifactStore>
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private FileManager fileManager;

    protected AbstractSimpleAccessResource()
    {
    }

    @GET
    @Path( "/{name}{path: (/.+)?}" )
    public Response getContent( @PathParam( "name" ) final String name, @PathParam( "path" ) final String path )
    {
        // TODO:
        // 1. directory request (ends with "/")...browse somehow??
        // 2. empty path (directory request for proxy root)

        Response response = null;

        ArtifactStore store = null;
        try
        {
            store = getArtifactStore( name );
        }
        catch ( final RESTWorkflowException e )
        {
            logger.error( "Failed to retrieve artifact store: %s. Reason: %s", e, name, e.getMessage() );
            response = e.getResponse();
        }

        if ( response == null )
        {
            if ( store == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                try
                {
                    final StorageItem item = fileManager.retrieve( store, path );
                    if ( item == null || item.isDirectory() )
                    {
                        response = Response.status( Status.NOT_FOUND )
                                           .build();
                    }
                    else
                    {
                        final String mimeType = new MimetypesFileTypeMap().getContentType( item.getPath() );
                        response = Response.ok( item.getStream(), mimeType )
                                           .build();
                    }
                }
                catch ( final RESTWorkflowException e )
                {
                    logger.error( "Failed to download artifact: %s from: %s. Reason: %s", e, path, name, e.getMessage() );
                    response = e.getResponse();
                }
            }
        }

        return response;
    }

    protected abstract T getArtifactStore( String name )
        throws RESTWorkflowException;

    protected FileManager getFileManager()
    {
        return fileManager;
    }

}
