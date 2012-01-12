package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.RepositoryDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;

public class MemoryRepositoryManagementTest
    extends RepositoryDataManagerTCK
{

    private final MemoryTCKFixtureProvider provider = new MemoryTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
