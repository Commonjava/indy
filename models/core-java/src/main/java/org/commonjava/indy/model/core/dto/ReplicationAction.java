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
package org.commonjava.indy.model.core.dto;

public class ReplicationAction
{
    public enum ActionType
    {
        PROXY, MIRROR;

        public static ActionType typeOf( final String name )
        {
            for ( final ActionType value : values() )
            {
                if ( value.name()
                          .equalsIgnoreCase( name ) )
                {
                    return value;
                }
            }

            return PROXY;
        }
    }

    private ActionType type;

    private String include;

    private String exclude;

    private String proxyHost;

    private int proxyPort;

    private String proxyUser;

    private String proxyPass;

    public ReplicationAction()
    {
        this.type = ActionType.PROXY;
    }

    public ReplicationAction( final ActionType type )
    {
        this.type = type;
    }

    public String getInclude()
    {
        return include;
    }

    public String getExclude()
    {
        return exclude;
    }

    public void setInclude( final String include )
    {
        this.include = include;
    }

    public void setExclude( final String exclude )
    {
        this.exclude = exclude;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyUser()
    {
        return proxyUser;
    }

    public String getProxyPass()
    {
        return proxyPass;
    }

    public void setProxyHost( final String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort( final int proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    public void setProxyUser( final String proxyUser )
    {
        this.proxyUser = proxyUser;
    }

    public void setProxyPass( final String proxyPass )
    {
        this.proxyPass = proxyPass;
    }

    public ActionType getType()
    {
        return type;
    }

    public void setType( final ActionType type )
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        return String.format( "ReplicationAction [type=%s, include=%s, exclude=%s, proxyHost=%s, proxyPort=%s, proxyUser=%s, proxyPass=%s]", type,
                              include, exclude, proxyHost, proxyPort, proxyUser, proxyPass );
    }
}
