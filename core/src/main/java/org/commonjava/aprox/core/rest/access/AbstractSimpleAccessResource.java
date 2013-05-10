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

import java.io.IOException;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.util.logging.Logger;

public abstract class AbstractSimpleAccessResource<T extends ArtifactStore>
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private FileManager fileManager;

    protected AbstractSimpleAccessResource()
    {
    }

    protected Response doDelete( final String name, final String path )
    {
        Response response = null;

        ArtifactStore store = null;
        try
        {
            store = getArtifactStore( name );
        }
        catch ( final AproxWorkflowException e )
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
                    if ( fileManager.delete( store, path ) )
                    {
                        response = Response.ok()
                                           .build();
                    }
                    else
                    {
                        response = Response.status( Status.NOT_FOUND )
                                           .build();
                    }
                }
                catch ( final AproxWorkflowException e )
                {
                    logger.error( "Failed to delete artifact: %s from: %s. Reason: %s", e, path, name, e.getMessage() );
                    response = e.getResponse();
                }
            }
        }

        return response;
    }

    protected Response doGet( final String name, final String path )
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
        catch ( final AproxWorkflowException e )
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
                        response = Response.ok( item.openInputStream(), mimeType )
                                           .build();
                    }
                }
                catch ( final AproxWorkflowException e )
                {
                    logger.error( "Failed to download artifact: %s from: %s. Reason: %s", e, path, name, e.getMessage() );
                    response = e.getResponse();
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to download artifact: %s from: %s. Reason: %s", e, path, name, e.getMessage() );
                    response = Response.serverError()
                                       .build();
                }
            }
        }

        return response;
    }

    protected abstract T getArtifactStore( String name )
        throws AproxWorkflowException;

    protected FileManager getFileManager()
    {
        return fileManager;
    }

}
