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
package org.commonjava.aprox.core.change.event;

import java.io.File;

import org.commonjava.aprox.core.model.ArtifactStore;

public class FileStorageEvent
{

    public enum Type
    {
        DOWNLOAD, GENERATE, UPLOAD;
    }

    private final Type type;

    private final ArtifactStore store;

    private final String path;

    private final String storageLocation;

    public FileStorageEvent( final Type type, final ArtifactStore store, final String path, final File storageLocation )
    {
        this.type = type;
        this.store = store;
        this.path = path;
        this.storageLocation = storageLocation.getAbsolutePath();
    }

    public FileStorageEvent( final Type type, final ArtifactStore store, final String path, final String storageLocation )
    {
        this.type = type;
        this.store = store;
        this.path = path;
        this.storageLocation = storageLocation;
    }

    public Type getType()
    {
        return type;
    }

    public String getPath()
    {
        return path;
    }

    public String getStorageLocation()
    {
        return storageLocation;
    }

    public ArtifactStore getStore()
    {
        return store;
    }

    @Override
    public String toString()
    {
        return String.format( "FileStorageEvent [type=%s, store=%s, path=%s, storageLocation=%s]", type, store, path,
                              storageLocation );
    }

}
