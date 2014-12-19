package org.commonjava.aprox.test.fixture.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CoreVertxServerFixtureTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void startAndStopWithNoOptions()
        throws Throwable
    {
        final CoreVertxServerFixture fix = new CoreVertxServerFixture( temp );
        fix.start();
        fix.stop();
    }

}
