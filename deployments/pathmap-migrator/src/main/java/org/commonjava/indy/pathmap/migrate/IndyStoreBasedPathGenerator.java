/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.pathmap.migrate;

import org.apache.commons.lang.StringUtils;
import org.commonjava.storage.pathmapped.util.PathMapUtils;

public class IndyStoreBasedPathGenerator
{
    private final String baseDir;

    IndyStoreBasedPathGenerator( final String baseDir )
    {
        this.baseDir = baseDir;
    }

    public String generatePath( String physicalPath )
    {
        final String storePath = generateStorePath( physicalPath );
        final String[] parts = storePath.split( "/" );
        String path = "";
        for ( int i = 2; i < parts.length; i++ )
        {
            path = PathMapUtils.normalize( path, parts[i] );
        }
        return path.startsWith( "/" ) ? path : "/" + path;
    }

    public String generateFileSystem( String physicalPath )
    {
        final String storePath = generateStorePath( physicalPath );
        final String[] parts = storePath.split( "/" );
        final String pkg = parts[0];
        final String repo = parts[1];
        final String[] keyAName = repo.split( "-" );
        final String type = keyAName[0];
        StringBuilder name = new StringBuilder();
        if ( keyAName.length > 2 )
        {
            for ( int i = 1; i < keyAName.length; i++ )
            {
                if ( name.length() != 0 )
                {
                    name.append( "-" );
                }
                name.append( keyAName[i] );
            }

        }
        else
        {
            name.append( keyAName[1] );
        }
        return pkg + ":" + type + ":" + name.toString();
    }

    public String generateStorePath( String physicalPath )
    {
        if ( StringUtils.isBlank( baseDir ) || !physicalPath.contains( baseDir ) )
        {
            return physicalPath;
        }

        final int storePathStart = physicalPath.indexOf( baseDir ) + baseDir.length();
        final String storePath = physicalPath.substring( storePathStart );
        return storePath.startsWith( "/" ) ? storePath.substring( 1 ) : storePath;
    }
}
