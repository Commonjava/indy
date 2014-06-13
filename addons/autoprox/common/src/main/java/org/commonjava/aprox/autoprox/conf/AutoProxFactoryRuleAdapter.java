package org.commonjava.aprox.autoprox.conf;

import java.net.MalformedURLException;

import org.commonjava.aprox.autoprox.data.AbstractAutoProxRule;
import org.commonjava.aprox.autoprox.data.AutoProxRuleException;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;

/**
 * @deprecated This is only provided as a shim to allow legacy rules to work.
 */
@Deprecated
public class AutoProxFactoryRuleAdapter
    extends AbstractAutoProxRule
{

    private final AutoProxFactory factory;

    public AutoProxFactoryRuleAdapter( final AutoProxFactory factory )
    {
        this.factory = factory;
    }

    @Override
    public boolean matches( final String name )
    {
        // Never called...the match should be in the RuleMapping that wraps this rule.
        return false;
    }

    @Override
    public RemoteRepository createRemoteRepository( final String named )
        throws AutoProxRuleException, MalformedURLException
    {
        return factory.createRemoteRepository( named );
    }

    @Override
    public HostedRepository createHostedRepository( final String named )
    {
        return factory.createHostedRepository( named );
    }

    @Override
    public Group createGroup( final String named )
    {
        RemoteRepository remote = null;
        try
        {
            remote = factory.createRemoteRepository( named );
        }
        catch ( final MalformedURLException e )
        {
        }

        final HostedRepository hosted = factory.createHostedRepository( named );

        return factory.createGroup( named, remote, hosted );
    }

    @Override
    public String getRemoteValidationPath()
    {
        return factory.getRemoteValidationPath();
    }

}
