/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.npm.model;

import io.swagger.annotations.ApiModel;

import java.util.HashMap;
import java.util.Map;

@ApiModel( description = "Indicate the structure of the package." )
public class Directories
{
    private static final String LIB = "lib";

    private static final String BIN = "bin";

    private static final String MAN = "man";

    private static final String DOC = "doc";

    private static final String EXAMPLE = "example";

    private static final String TEST = "test";

    private Map<String, Object> directoriesMap = new HashMap<String, Object>();

    protected Directories()
    {
    }

    public Object getLib()
    {
        return directoriesMap.get( LIB );
    }

    public void setLib( Object lib )
    {
        directoriesMap.put( LIB, lib );
    }

    public Object getBin()
    {
        return directoriesMap.get( BIN );
    }

    public void setBin( Object bin )
    {
        directoriesMap.put( BIN, bin );
    }

    public Object getMan()
    {
        return directoriesMap.get( MAN );
    }

    public void setMan( Object man )
    {
        directoriesMap.put( MAN, man );
    }

    public Object getDoc()
    {
        return directoriesMap.get( DOC );
    }

    public void setDoc( Object doc )
    {
        directoriesMap.put( DOC, doc );
    }

    public Object getExample()
    {
        return directoriesMap.get( EXAMPLE );
    }

    public void setExample( Object example )
    {
        directoriesMap.put( EXAMPLE, example );
    }

    public Object getTest()
    {
        return directoriesMap.get( TEST );
    }

    public void setTest( Object test )
    {
        directoriesMap.put( TEST, test );
    }

    public Map<String, Object> fetchDirectoriesMap()
    {
        return directoriesMap;
    }

    public Object getDirectory( String name )
    {
        return directoriesMap.get( name );
    }

    public void putDirectory( String name, Object value )
    {
        directoriesMap.put( name, value );
    }
}
