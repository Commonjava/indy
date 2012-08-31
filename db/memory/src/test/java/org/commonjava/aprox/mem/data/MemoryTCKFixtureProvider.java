package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.data.StoreDataManager;

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
