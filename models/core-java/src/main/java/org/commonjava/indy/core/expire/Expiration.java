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
package org.commonjava.indy.core.expire;

import java.util.Date;

/**
 * Created by jdcasey on 1/4/16.
 */
public class Expiration
{

    private String name;

    private String group;

    private Date expiration;

    public Expiration(){}

    public Expiration( String group, String name, Date expiration )
    {
        this.group = group;
        this.name = name;
        this.expiration = expiration;
    }

    public Expiration( String group, String name )
    {
        this.group = group;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public Date getExpiration()
    {
        return expiration;
    }

    public void setExpiration( Date expiration )
    {
        this.expiration = expiration;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof Expiration ) )
        {
            return false;
        }

        Expiration that = (Expiration) o;

        if ( getName() != null ? !getName().equals( that.getName() ) : that.getName() != null )
        {
            return false;
        }
        return !( getGroup() != null ? !getGroup().equals( that.getGroup() ) : that.getGroup() != null );

    }

    @Override
    public int hashCode()
    {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + ( getGroup() != null ? getGroup().hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "Expiration{" +
                "name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", expiration=" + expiration +
                '}';
    }
}
