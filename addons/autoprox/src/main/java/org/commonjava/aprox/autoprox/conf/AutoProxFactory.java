package org.commonjava.aprox.autoprox.conf;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;

public interface AutoProxFactory
{

    String LEGACY_FACTORY_NAME = "legacy-factory.groovy";

    String DEFAULT_FACTORY_SCRIPT = "default.groovy";

    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException;

    HostedRepository createHostedRepository( String named );

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted );

    String getRemoteValidationPath();

}
