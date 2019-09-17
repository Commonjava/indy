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

public class UserInfo
{
    private final String name;

    private final String email;

    private final String url;

    protected UserInfo()
    {
        this.name = null;
        this.email = null;
        this.url = null;
    }

    public UserInfo( final String name )
    {
        this.name = name;
        this.email = null;
        this.url = null;
    }

    public UserInfo( final String name, final String email )
    {
        this.name = name;
        this.email = email;
        this.url = null;
    }

    public UserInfo( final String name, final String email, final String url )
    {
        this.name = name;
        this.email = email;
        this.url = url;
    }

    public String getName()
    {
        return name;
    }

    public String getEmail()
    {
        return email;
    }

    public String getUrl()
    {
        return url;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof UserInfo ) )
        {
            return false;
        }

        UserInfo that = (UserInfo) o;

        return getName().equals( that.getName() );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }
}
