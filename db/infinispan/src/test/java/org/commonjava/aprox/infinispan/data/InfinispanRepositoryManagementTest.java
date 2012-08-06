package org.commonjava.aprox.infinispan.data;

import org.commonjava.aprox.core.data.RepositoryDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;

public class InfinispanRepositoryManagementTest
    extends RepositoryDataManagerTCK
{

    public InfinispanTCKFixtureProvider provider = new InfinispanTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
