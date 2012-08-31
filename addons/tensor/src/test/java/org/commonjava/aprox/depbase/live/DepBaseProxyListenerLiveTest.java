package org.commonjava.aprox.depbase.live;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class DepBaseProxyListenerLiveTest
    extends AbstractAProxDepbaseLiveTest
{

    @Deployment
    public static WebArchive createWar()
    {
        return createWar( DepBaseProxyListenerLiveTest.class );
    }

    @Test
    public void injectDependenciesOfDownloadedPOM()
        throws Exception
    {
        proxyManager.storeRepository( new Repository( "central", "http://repo.maven.apache.org/maven2/" ) );
        proxyManager.storeGroup( new Group( "test", new StoreKey( StoreType.repository, "central" ) ) );

        webFixture.get( webFixture.resourceUrl( "group", "test",
                                                "org/apache/maven/maven-core/3.0.3/maven-core-3.0.3.pom" ), 200 );

        // dataManager.get
    }

}
