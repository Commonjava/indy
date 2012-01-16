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
package org.commonjava.aprox.core.rest.util.retrieve;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.util.FileManager;

@Singleton
public class GroupHandlerChain
{

    @Inject
    private Instance<GroupPathHandler> handlers;

    @Inject
    private FileManager downloader;

    public File retrieve( final Group group, final List<? extends ArtifactStore> stores, final String path )
    {
        for ( final GroupPathHandler handler : handlers )
        {
            if ( handler.canHandle( path ) )
            {
                return handler.retrieve( group, stores, path );
            }
        }

        return downloader.downloadFirst( stores, path );
    }

    public DeployPoint store( final Group group, final List<? extends ArtifactStore> stores, final String path,
                              final InputStream stream )
    {
        for ( final GroupPathHandler handler : handlers )
        {
            if ( handler.canHandle( path ) )
            {
                return handler.store( group, stores, path, stream );
            }
        }

        return downloader.upload( stores, path, stream );
    }

}
