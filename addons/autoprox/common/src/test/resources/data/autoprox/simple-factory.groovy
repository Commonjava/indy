
import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;
import org.commonjava.aprox.autoprox.data.*;

class ProdFactory extends AbstractAutoProxRule
{

    RemoteRepository createRemoteRepository( final String named )
        throws MalformedURLException
    {
        String baseUrl = System.getProperty( "baseUrl" );
        new RemoteRepository( named, baseUrl + "/target/" + named );
    }

    HostedRepository createHostedRepository( final String named )
    {
        new HostedRepository( named );
    }

    Group createGroup( final String named )
    {
        List<StoreKey> constituents = new ArrayList<StoreKey>();
        
        constituents.add( new StoreKey( StoreType.hosted, named ) );
        constituents.add( new StoreKey( StoreType.remote, named ) );
        constituents.add( new StoreKey( StoreType.remote, "first" ) );
        constituents.add( new StoreKey( StoreType.remote, "second" ) );
        
        new Group( named, constituents );
    }

    boolean matches( final String name )
    {
        "test".equals( name );
    }
    
}