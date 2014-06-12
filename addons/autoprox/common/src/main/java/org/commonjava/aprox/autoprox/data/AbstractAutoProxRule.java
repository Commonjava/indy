package org.commonjava.aprox.autoprox.data;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;

public abstract class AbstractAutoProxRule
    implements AutoProxRule
{

    @Override
    public RemoteRepository createRemoteRepository( final String named )
        throws AutoProxRuleException, MalformedURLException
    {
        return null;
    }

    @Override
    public HostedRepository createHostedRepository( final String named )
    {
        return null;
    }

    @Override
    public Group createGroup( final String named )
    {
        return null;
    }

    @Override
    public String getRemoteValidationPath()
    {
        return null;
    }

    @Override
    public RemoteRepository createGroupValidationRemote( final String name )
        throws AutoProxRuleException, MalformedURLException
    {
        return createRemoteRepository( name );
    }

}
