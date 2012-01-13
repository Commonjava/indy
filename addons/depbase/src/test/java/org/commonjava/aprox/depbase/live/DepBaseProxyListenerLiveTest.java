package org.commonjava.aprox.depbase.live;

import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
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
        proxyManager.storeRepository( modelFactory.createRepository( "central", "http://repo.maven.apache.org/maven2/" ) );
        proxyManager.storeGroup( modelFactory.createGroup( "test", new StoreKey( StoreType.repository, "central" ) ) );

        webFixture.get( webFixture.resourceUrl( "group", "test",
                                                "org/apache/maven/maven-core/3.0.3/maven-core-3.0.3.pom" ), 200 );

        // dataManager.get
    }

}
