package org.commonjava.aprox.subsys.threads.inject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.commonjava.aprox.test.utils.WeldJUnit4Runner;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( WeldJUnit4Runner.class )
public class ExecutorSvcWithConfigProviderWeldTest
{

    @Inject
    @ExecutorConfig( threads = 2, named = "test" )
    private ExecutorService service;

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Test
    public void testExecutorServiceInjection()
    {
        assertThat( service, notNullValue() );
    }

}
