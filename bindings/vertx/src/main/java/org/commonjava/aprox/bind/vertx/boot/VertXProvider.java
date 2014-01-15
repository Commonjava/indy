package org.commonjava.aprox.bind.vertx.boot;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;

@ApplicationScoped
public class VertXProvider
{

    private MemoryStoreDataManager storeManager;

    public VertXProvider()
    {
    }

    //    @Produces
    //    @Default
    public synchronized StoreDataManager getStoreManager()
    {
        if ( storeManager == null )
        {
            storeManager = new MemoryStoreDataManager();
        }

        return storeManager;
    }

}
