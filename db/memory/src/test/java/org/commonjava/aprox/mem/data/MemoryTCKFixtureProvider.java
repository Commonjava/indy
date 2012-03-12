package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.mem.model.MemoryModelFactory;

public class MemoryTCKFixtureProvider
    implements TCKFixtureProvider
{

    private final MemoryStoreDataManager dataManager = new MemoryStoreDataManager();

    private final MemoryModelFactory factory = new MemoryModelFactory();

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

    @Override
    public ModelFactory getModelFactory()
    {
        return factory;
    }

}
