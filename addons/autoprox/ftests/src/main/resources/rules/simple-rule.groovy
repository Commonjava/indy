import org.commonjava.indy.model.core.*;
import org.commonjava.indy.autoprox.data.*;

class SimpleRule extends AbstractAutoProxRule
{

    RemoteRepository createRemoteRepository( final StoreKey key )
        throws MalformedURLException
    {
        String baseUrl = System.getProperty( "baseUrl" );
        new RemoteRepository( key.getName(), "http://localhost:1000/target/" + key.getName() );
    }

    HostedRepository createHostedRepository( final StoreKey key )
    {
        HostedRepository r = new HostedRepository( key.getPackageType(), key.getName() );
        r.setAllowSnapshots(true);
        r.setAllowReleases(true);
        
        r
    }

    Group createGroup( final StoreKey key )
    {
        List<StoreKey> constituents = new ArrayList<StoreKey>();
        
        constituents.add( new StoreKey( key.getPackageType(), StoreType.hosted, key.getName() ) );
        constituents.add( new StoreKey( key.getPackageType(), StoreType.remote, key.getName() ) );
        constituents.add( new StoreKey( key.getPackageType(), StoreType.remote, "test-first" ) );
        constituents.add( new StoreKey( key.getPackageType(), StoreType.remote, "test-second" ) );
        
        new Group( key.getPackageType(), key.getName(), constituents );
    }

    boolean matches( final StoreKey key )
    {
        key.getName().startsWith( "test" );
    }
    
}