package org.commonjava.aprox.autoprox.conf;

import java.net.MalformedURLException;

import org.commonjava.aprox.autoprox.data.AbstractAutoProxRule;
import org.commonjava.aprox.autoprox.data.AutoProxRule;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;

/**
 * @deprecated Use {@link AutoProxRule} and {@link AbstractAutoProxRule} instead.
 */
@Deprecated
public interface AutoProxFactory
{
    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException;

    HostedRepository createHostedRepository( String named );

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted );

    String getRemoteValidationPath();
}
