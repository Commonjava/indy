package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.data.TCKFixtureProvider;

public class MemoryTCKFixtureProvider
    implements TCKFixtureProvider
{

    private final MemoryStoreDataManager dataManager = new MemoryStoreDataManager();

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

}
