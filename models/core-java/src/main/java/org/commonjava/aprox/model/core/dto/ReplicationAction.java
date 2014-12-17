/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.model.core.dto;

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
        return String.format( "ReplicationAction [type=%s, include=%s, exclude=%s, proxyHost=%s, proxyPort=%s, proxyUser=%s, proxyPass=%s]", type,
                              include, exclude, proxyHost, proxyPort, proxyUser, proxyPass );
    }
}
