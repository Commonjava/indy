package org.commonjava.aprox.autoprox.conf;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;

public interface AutoProxFactory
{

    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException;

    HostedRepository createHostedRepository( String named );

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted );

    String getRemoteValidationPath();

}
