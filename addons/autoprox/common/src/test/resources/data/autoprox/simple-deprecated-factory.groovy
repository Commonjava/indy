import org.commonjava.indy.model.core.*;
import org.commonjava.indy.autoprox.data.*;

class TestDeprecatedFactory extends AbstractAutoProxRule
{

    RemoteRepository createRemoteRepository( final String name )
        throws MalformedURLException
    {
        String baseUrl = System.getProperty( "baseUrl" );
        new RemoteRepository( name, baseUrl + "/target/" + name );
    }

    HostedRepository createHostedRepository( final String name )
    {
        new HostedRepository( name );
    }

    Group createGroup( String name )
    {
        List<StoreKey> constituents = new ArrayList<StoreKey>();
        
        constituents.add( new StoreKey( StoreType.hosted, name ) );
        constituents.add( new StoreKey( StoreType.remote, name ) );
        constituents.add( new StoreKey( StoreType.remote, "first" ) );
        constituents.add( new StoreKey( StoreType.remote, "second" ) );
        
        new Group( name, constituents );
    }

    boolean matches( final String name )
    {
        "test".equals( name );
    }
    
}