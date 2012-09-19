package org.commonjava.aprox.rest.util;

import org.commonjava.aprox.core.data.FilerTCK;
import org.commonjava.aprox.io.StorageProvider;
import org.infinispan.Cache;
import org.infinispan.io.GridFile;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Before;

public class GridStorageProviderTest
    extends FilerTCK
{

    private CacheContainer container;

    private GridStorageProvider provider;

    @Before
    public void setup()
    {
        container = new DefaultCacheManager();
        container.start();

        final Cache<String, GridFile.Metadata> metadata = container.getCache( "metadata" );
        metadata.start();

        final Cache<String, byte[]> data = container.getCache( "data" );
        data.start();

        provider = new GridStorageProvider( metadata, data );
    }

    @After
    public void teardown()
    {
        if ( container != null )
        {
            container.stop();
        }
    }

    @Override
    protected StorageProvider getStorageProvider()
        throws Exception
    {
        return provider;
    }
}
