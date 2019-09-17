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

    private Map<String, String> directoriesMap = new HashMap<String, String>();

    protected Directories()
    {
    }

    public String getLib()
    {
        return directoriesMap.get( LIB );
    }

    public void setLib( String lib )
    {
        directoriesMap.put( LIB, lib );
    }

    public String getBin()
    {
        return directoriesMap.get( BIN );
    }

    public void setBin( String bin )
    {
        directoriesMap.put( BIN, bin );
    }

    public String getMan()
    {
        return directoriesMap.get( MAN );
    }

    public void setMan( String man )
    {
        directoriesMap.put( MAN, man );
    }

    public String getDoc()
    {
        return directoriesMap.get( DOC );
    }

    public void setDoc( String doc )
    {
        directoriesMap.put( DOC, doc );
    }

    public String getExample()
    {
        return directoriesMap.get( EXAMPLE );
    }

    public void setExample( String example )
    {
        directoriesMap.put( EXAMPLE, example );
    }

    public String getTest()
    {
        return directoriesMap.get( TEST );
    }

    public void setTest( String test )
    {
        directoriesMap.put( TEST, test );
    }

    public Map<String, String> fetchDirectoriesMap()
    {
        return directoriesMap;
    }

    public String getDirectory( String name )
    {
        return directoriesMap.get( name );
    }

    public void putDirectory( String name, String value )
    {
        directoriesMap.put( name, value );
    }
}
