
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;

class SonatypeFactory implements AutoProxFactory
{
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

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted )
    {
        Group g = new Group( named );
        g.addConstituent( new StoreKey( StoreType.remote, named ) )
/*        g.addConstituent( new StoreKey( StoreType.hosted, named ) )*/
        
        g
    }

    String getRemoteValidationPath()
    {
        null
    }
}