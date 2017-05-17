import org.commonjava.indy.model.core.*;
import org.commonjava.indy.autoprox.data.*;

class TestFactory extends AbstractAutoProxRule
{

    RemoteRepository createRemoteRepository( final StoreKey key )
        throws MalformedURLException
    {
        String baseUrl = System.getProperty( "baseUrl" );
        new RemoteRepository( key.getPackageType(), key.getName(), baseUrl + "/target/" + key.getName() );
    }

    HostedRepository createHostedRepository( final StoreKey key )
    {
        new HostedRepository( key.getPackageType(), key.getName() );
    }

    Group createGroup( StoreKey key )
    {
        List<StoreKey> constituents = new ArrayList<StoreKey>();
        
        constituents.add( new StoreKey( key.getPackageType(), StoreType.hosted, key.getName() ) );
        constituents.add( new StoreKey( key.getPackageType(), StoreType.remote, key.getName() ) );
        constituents.add( new StoreKey( key.getPackageType(), StoreType.remote, "first" ) );
        constituents.add( new StoreKey( key.getPackageType(), StoreType.remote, "second" ) );
        
        new Group( key.getPackageType(), key.getName(), constituents );
    }

    boolean matches( final StoreKey key )
    {
        "test".equals( key.getName() );
    }
    
}