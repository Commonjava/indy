
import java.net.MalformedURLException;

import org.commonjava.aprox.model.core.*;
import org.commonjava.aprox.autoprox.data.*;

class ProdFactory extends AbstractAutoProxRule
{

    boolean matches( String named )
    {
        true;
    }
    
    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        def match = (named =~ /prod-([^0-9]+)([0-9])(.+)/)[0]
        new RemoteRepository( name: named, url: "http://repository.myco.com/products/${match[1] + match[2]}/${match[2] + match[3]}/" )
    }

    HostedRepository createHostedRepository( String named )
    {
        new HostedRepository( named )
    }

    Group createGroup( String named )
    {
        Group g = new Group( named );
        g.addConstituent( new StoreKey( StoreType.remote, "central" ) )
        g.addConstituent( new StoreKey( StoreType.remote, named ) )
        g.addConstituent( new StoreKey( StoreType.hosted, named ) )
        
        g
    }
}