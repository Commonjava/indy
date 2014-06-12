
import org.commonjava.aprox.autoprox.data.*;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;

class RedHatRule extends AbstractAutoProxRule
{
    boolean matches( String name ){
        name.startsWith( "RH-" )
    }

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

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted )
    {
        Group g = new Group( named );
        g.addConstituent( new StoreKey( StoreType.remote, named ) )
        
        g
    }
}
