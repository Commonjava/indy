package org.commonjava.aprox.infinispan.data;

import org.commonjava.aprox.core.data.GroupDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;

public class InfinispanGroupManagementTest
    extends GroupDataManagerTCK
{

    public InfinispanTCKFixtureProvider provider = new InfinispanTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
