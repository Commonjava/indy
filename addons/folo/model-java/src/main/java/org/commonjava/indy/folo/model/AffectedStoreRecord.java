/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.folo.model;

import java.util.Set;
import java.util.TreeSet;

import org.commonjava.indy.model.core.StoreKey;

@Deprecated
public class AffectedStoreRecord
{

    private StoreKey key;

    private Set<String> uploadedPaths;

    private Set<String> downloadedPaths;

    protected AffectedStoreRecord()
    {
    }

    protected void setKey( final StoreKey key )
    {
        this.key = key;
    }

    public AffectedStoreRecord( final StoreKey key )
    {
        this.key = key;
    }

    public StoreKey getKey()
    {
        return key;
    }

    public Set<String> getDownloadedPaths()
    {
        return downloadedPaths;
    }

    public Set<String> getUploadedPaths()
    {
        return uploadedPaths;
    }

    public synchronized void add( final String path, final StoreEffect type )
    {
        if ( path == null )
        {
            return;
        }

        if ( type == StoreEffect.DOWNLOAD )
        {
            if ( downloadedPaths == null )
            {
                downloadedPaths = new TreeSet<>();
            }

            downloadedPaths.add( path );
        }
        else
        {
            if ( uploadedPaths == null )
            {
                uploadedPaths = new TreeSet<>();
            }

            uploadedPaths.add( path );
        }
    }

}
