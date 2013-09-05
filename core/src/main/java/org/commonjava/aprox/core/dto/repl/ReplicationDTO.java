package org.commonjava.aprox.core.dto.repl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.rest.AproxWorkflowException;

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
        throws AproxWorkflowException
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
            throw new AproxWorkflowException( Status.BAD_REQUEST, sb.toString() );
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
