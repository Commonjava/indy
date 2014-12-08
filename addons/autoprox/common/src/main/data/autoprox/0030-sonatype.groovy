
import org.commonjava.aprox.autoprox.data.*;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.core.*;

class SonatypeRule extends AbstractAutoProxRule
{
    boolean matches( String named ){
        named.startsWith( "ST-" )
    }

    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        def match = (named =~ /ST-(.+)/)[0]
        new RemoteRepository( name: named, url: "http://oss.sonatype.org/content/repositories/${match[1]}/" )
    }

    HostedRepository createHostedRepository( String named )
    {
/*        new HostedRepository( named )*/
        null
    }

    Group createGroup( String named )
    {
        Group g = new Group( named );
        g.addConstituent( new StoreKey( StoreType.remote, named ) )
/*        g.addConstituent( new StoreKey( StoreType.hosted, named ) )*/
        
        g
    }
}
