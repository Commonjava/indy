
import org.commonjava.aprox.autoprox.data.*;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JBossOrgRule extends AbstractAutoProxRule
{
    boolean matches( String named ){
        named.startsWith( "JB-" )
    }

    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        def match = (named =~ /JB-(.+)/)[0]
        return new RemoteRepository( name: named, url: "https://repository.jboss.org/nexus/content/repositories/${match[1]}/" )
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
        
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Created group: {}", g )

        return g
    }
}
