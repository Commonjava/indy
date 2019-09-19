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
package org.commonjava.indy.subsys.prefetch.models;

public class RescanablePath
{
    private String path;

    private Boolean rescan;

    public RescanablePath( String path, Boolean rescan )
    {
        this.path = path;
        this.rescan = rescan;
    }

    public String getPath()
    {
        return path;
    }

    public Boolean isRescan()
    {
        return rescan;
    }

    @Override
    public int hashCode()
    {
        return path.hashCode();
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof RescanablePath ) )
        {
            return false;
        }
        RescanablePath that = (RescanablePath) obj;
        if ( this.path == null && that.path == null )
        {
            return true;
        }
        else if ( this.path != null && that.path != null )
        {
            return this.path.equals( that.path );
        }
        else
        {
            return false;
        }

    }

    @Override
    public String toString()
    {
        return this.path + ", rescan=" + this.rescan;
    }
}
