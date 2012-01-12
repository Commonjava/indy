package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.GroupDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;

public class MemoryGroupManagementTest
    extends GroupDataManagerTCK
{

    private final MemoryTCKFixtureProvider provider = new MemoryTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
