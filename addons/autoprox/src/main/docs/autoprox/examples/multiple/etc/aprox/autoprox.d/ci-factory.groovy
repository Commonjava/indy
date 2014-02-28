
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;

class CiFactory implements AutoProxFactory
{
    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        new RemoteRepository( name: named, url: "http://ci.myco.com/repos/$named/" )
    }

    HostedRepository createHostedRepository( String named )
    {
        new HostedRepository( named )
    }

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted )
    {
        Group g = new Group( named );
        g.addConstituent( new StoreKey( StoreType.remote, "central" ) )
        g.addConstituent( new StoreKey( StoreType.remote, named ) )
        g.addConstituent( new StoreKey( StoreType.hosted, named ) )
        
        g
    }

    String getRemoteValidationPath()
    {
        null
    }
}