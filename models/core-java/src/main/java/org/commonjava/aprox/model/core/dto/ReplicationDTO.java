/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.model.core.dto;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.commonjava.aprox.AproxException;

public class ReplicationDTO
    implements Iterable<ReplicationAction>
{

    private boolean overwrite;

    private String apiUrl;

    private String proxyHost;

    private int proxyPort;

    private List<ReplicationAction> actions;

    public String getApiUrl()
    {
        return apiUrl;
    }

    public List<ReplicationAction> getActions()
    {
        return actions;
    }

    public void setApiUrl( final String apiUrl )
    {
        this.apiUrl = apiUrl;
    }

    public void setActions( final List<ReplicationAction> actions )
    {
        this.actions = actions;
    }

    public boolean isOverwrite()
    {
        return overwrite;
    }

    public void setOverwrite( final boolean overwrite )
    {
        this.overwrite = overwrite;
    }

    public void validate()
        throws AproxException
    {
        final StringBuilder sb = new StringBuilder();
        if ( apiUrl == null )
        {
            sb.append( "You must provide an api URL to the remote Aprox instance." );
        }
        else
        {
            try
            {
                new URL( apiUrl );
            }
            catch ( final MalformedURLException e )
            {
                sb.append( "You must provide a VALID api URL to the remote Aprox instance. (invalid: '" )
                  .append( apiUrl )
                  .append( "'): " )
                  .append( e.getMessage() );
            }
        }

        if ( actions == null || actions.isEmpty() )
        {
            actions = Collections.<ReplicationAction> singletonList( new ReplicationAction() );
        }

        if ( sb.length() > 0 )
        {
            throw new AproxException( sb.toString() );
        }
    }

    @Override
    public Iterator<ReplicationAction> iterator()
    {
        return actions.iterator();
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public void setProxyHost( final String proxyHost )
    {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort( final int proxyPort )
    {
        this.proxyPort = proxyPort;
    }

    @Override
    public String toString()
    {
        return String.format( "ReplicationDTO [overwrite=%s, apiUrl=%s, proxyHost=%s, proxyPort=%s, actions=%s]", overwrite, apiUrl, proxyHost,
                              proxyPort, actions );
    }

}
