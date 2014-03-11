
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;

class RedHatFactory implements AutoProxFactory
{
    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        if ( named == "RH-all" ){
          new RemoteRepository( name: named, url: "http://maven.repository.redhat.com/techpreview/all/" )
        }
        else{
          def match = (named =~ /RH-([^0-9]+)([0-9])(.+)/)[0]
          new RemoteRepository( name: named, url: "http://maven.repository.redhat.com/techpreview/${match[1] + match[2]}/${match[2] + match[3]}/maven-repository/" )
        }
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