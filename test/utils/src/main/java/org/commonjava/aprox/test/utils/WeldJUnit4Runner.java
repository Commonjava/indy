package org.commonjava.aprox.test.utils;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class WeldJUnit4Runner
    extends BlockJUnit4ClassRunner
{

    private Weld weld;

    private WeldContainer container;

    public WeldJUnit4Runner( final Class<?> klass )
        throws InitializationError
    {
        super( klass );
    }

    @Override
    protected Object createTest()
        throws Exception
    {
        this.weld = new Weld();
        this.container = weld.initialize();
        return container.instance()
                        .select( getTestClass().getJavaClass() )
                        .get();
    }

    @Override
    public void run( final RunNotifier notifier )
    {
        try
        {
            super.run( notifier );
        }
        finally
        {
            if ( container != null )
            {
                weld.shutdown();
            }
        }
    }

}
