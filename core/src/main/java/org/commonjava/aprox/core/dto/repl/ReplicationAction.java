/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.dto.repl;

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

    private String proxyHost;

    private int proxyPort;

    private String proxyUser;

    private String proxyPass;

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
        return String.format( "ReplicationAction [type={}, include={}, exclude={}, proxyHost={}, proxyPort={}, proxyUser={}, proxyPass={}]", type,
                              include, exclude, proxyHost, proxyPort, proxyUser, proxyPass );
    }
}
