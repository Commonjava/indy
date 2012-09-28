package org.commonjava.aprox.rest.util;

import org.commonjava.aprox.core.data.FilerTCK;
import org.commonjava.aprox.io.StorageProvider;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;

public class GridStorageProviderWeldTest
    extends FilerTCK
{

    private GridStorageProvider provider;

    private Weld weld;

    @Before
    public void setup()
    {
        weld = new Weld();
        final WeldContainer container = weld.initialize();

        provider = container.instance()
                            .select( GridStorageProvider.class )
                            .get();
    }

    @After
    public void teardown()
    {
        if ( weld != null )
        {
            weld.shutdown();
        }
    }

    @Override
    protected StorageProvider getStorageProvider()
        throws Exception
    {
        return provider;
    }
}
