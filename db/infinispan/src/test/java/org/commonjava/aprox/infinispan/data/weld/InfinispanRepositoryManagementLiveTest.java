package org.commonjava.aprox.infinispan.data.weld;

import org.commonjava.aprox.core.data.RepositoryDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;

public class InfinispanRepositoryManagementLiveTest
    extends RepositoryDataManagerTCK
{

    private InfinispanTCKLiveFixtureProvider provider;

    private Weld weld;

    @Before
    public void setup()
    {
        weld = new Weld();
        final WeldContainer container = weld.initialize();

        provider = container.instance()
                            .select( InfinispanTCKLiveFixtureProvider.class )
                            .get();
    }

    public void teardown()
    {
        if ( weld != null )
        {
            weld.shutdown();
        }
    }

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
