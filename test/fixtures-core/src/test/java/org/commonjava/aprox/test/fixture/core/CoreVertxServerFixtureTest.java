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
        final CoreServerFixture fix = new CoreServerFixture( temp );
        fix.start();
        fix.stop();
    }

}
