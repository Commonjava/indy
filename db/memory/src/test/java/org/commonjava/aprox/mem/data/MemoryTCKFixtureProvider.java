package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.mem.model.MemoryModelFactory;

public class MemoryTCKFixtureProvider
    implements TCKFixtureProvider
{

    private final MemoryProxyDataManager dataManager = new MemoryProxyDataManager();

    private final MemoryModelFactory factory = new MemoryModelFactory();

    @Override
    public ProxyDataManager getDataManager()
    {
        return dataManager;
    }

    @Override
    public ModelFactory getModelFactory()
    {
        return factory;
    }

}
