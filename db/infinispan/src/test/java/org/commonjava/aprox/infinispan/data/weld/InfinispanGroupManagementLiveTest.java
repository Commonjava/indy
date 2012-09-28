package org.commonjava.aprox.infinispan.data.weld;

import org.commonjava.aprox.core.data.GroupDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;

public class InfinispanGroupManagementLiveTest
    extends GroupDataManagerTCK
{

    private InfinispanTCKLiveFixtureProvider provider;

    private Weld weld;

    @Override
    protected void doSetup()
        throws Exception
    {
        weld = new Weld();
        final WeldContainer container = weld.initialize();

        provider = container.instance()
                            .select( InfinispanTCKLiveFixtureProvider.class )
                            .get();
    }

    @After
    public void doTeardown()
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
