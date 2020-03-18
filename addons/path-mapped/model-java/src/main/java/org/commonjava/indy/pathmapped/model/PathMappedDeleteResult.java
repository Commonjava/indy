/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pathmapped.model;

public class PathMappedDeleteResult
{
    private String packageType;

    private String type;

    private String name;

    private String path;

    boolean result;

    public PathMappedDeleteResult()
    {
    }

    public PathMappedDeleteResult( String packageType, String type, String name, String path, boolean result )
    {
        this.packageType = packageType;
        this.type = type;
        this.name = name;
        this.path = path;
        this.result = result;
    }

    public String getPackageType()
    {
        return packageType;
    }

    public void setPackageType( String packageType )
    {
        this.packageType = packageType;
    }

    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public boolean isResult()
    {
        return result;
    }

    public void setResult( boolean result )
    {
        this.result = result;
    }
}
