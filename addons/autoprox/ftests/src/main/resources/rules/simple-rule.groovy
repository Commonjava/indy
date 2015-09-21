
import java.net.MalformedURLException;

import org.commonjava.aprox.model.core.*;
import org.commonjava.aprox.autoprox.data.*;

class SimpleRule extends AbstractAutoProxRule
{

    RemoteRepository createRemoteRepository( final String named )
        throws MalformedURLException
    {
        String baseUrl = System.getProperty( "baseUrl" );
        new RemoteRepository( named, "http://localhost:1000/target/" + named );
    }

    HostedRepository createHostedRepository( final String named )
    {
        HostedRepository r = new HostedRepository( named );
        r.setAllowSnapshots(true);
        r.setAllowReleases(true);
        
        r
    }

    Group createGroup( final String named )
    {
        List<StoreKey> constituents = new ArrayList<StoreKey>();
        
        constituents.add( new StoreKey( StoreType.hosted, named ) );
        constituents.add( new StoreKey( StoreType.remote, named ) );
        constituents.add( new StoreKey( StoreType.remote, "test-first" ) );
        constituents.add( new StoreKey( StoreType.remote, "test-second" ) );
        
        new Group( named, constituents );
    }

    boolean matches( final String name )
    {
        name.startsWith( "test" );
    }
    
}