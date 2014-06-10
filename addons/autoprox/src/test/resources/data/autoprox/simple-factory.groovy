
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;

class ProdFactory implements AutoProxFactory
{

    @Override
    public RemoteRepository createRemoteRepository( final String named )
        throws MalformedURLException
    {
        String baseUrl = System.getProperty( "baseUrl" );
        return new RemoteRepository( named, baseUrl + "/target/" + named );
    }

    @Override
    public HostedRepository createHostedRepository( final String named )
    {
        return new HostedRepository( named );
    }

    @Override
    public Group createGroup( final String named, final RemoteRepository remote, final HostedRepository hosted )
    {
        List<StoreKey> constituents = new ArrayList<StoreKey>();
        if ( hosted != null )
        {
            constituents.add( hosted.getKey() );
        }
        if ( remote != null )
        {
            constituents.add( remote.getKey() );
        }
        
        constituents.add( new StoreKey( StoreType.remote, "first" ) );
        constituents.add( new StoreKey( StoreType.remote, "second" ) );
        
        return new Group( named, constituents );
    }

    @Override
    public String getRemoteValidationPath()
    {
        return null;
    }

    @Override
    public boolean matches( final String name )
    {
        return "test".equals( name );
    }
    
}