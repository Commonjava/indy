package org.commonjava.aprox.autoprox.live.fixture;

import java.net.MalformedURLException;

import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.test.WebFixture;

public class TestAutoProxFactory
    implements AutoProxFactory
{

    private final WebFixture http;

    public TestAutoProxFactory( final WebFixture http )
    {
        this.http = http;
    }

    @Override
    public RemoteRepository createRemoteRepository( final String named )
        throws MalformedURLException
    {
        return new RemoteRepository( "repo", http.resourceUrl( "target/${name}" ) );
    }

    @Override
    public HostedRepository createHostedRepository( final String named )
    {
        return null;
    }

    @Override
    public Group createGroup( final String named, final RemoteRepository remote, final HostedRepository hosted )
    {
        return new Group( "group", new StoreKey( StoreType.remote, "first" ), new StoreKey( StoreType.remote, "second" ) );
    }

    @Override
    public String getRemoteValidationPath()
    {
        return null;
    }

}
