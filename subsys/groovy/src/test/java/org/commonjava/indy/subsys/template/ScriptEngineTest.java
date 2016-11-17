package org.commonjava.indy.subsys.template;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.test.utils.WeldJUnit4Runner;
import org.commonjava.indy.subsys.template.fixture.ScriptedThingOwner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 11/17/16.
 */
@RunWith( WeldJUnit4Runner.class )
public class ScriptEngineTest
{
    @Inject
    private ScriptEngine scriptEngine;

    @Test
    public void testParsedScriptFields_InjectionEnabled()
            throws IOException, IndyGroovyException
    {
        try (InputStream stream = Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream( "test-scripts/simple-injection.groovy" ))
        {
            String scriptSrc = IOUtils.toString( stream );
            ScriptedThingOwner owner =
                    scriptEngine.parseScriptInstance( scriptSrc, ScriptedThingOwner.class, true );

            assertThat( owner, notNullValue() );
            assertThat( owner.getThing(), notNullValue() );
        }
    }

    @Test
    public void testParsedScriptFields_InjectionDisabled()
            throws IOException, IndyGroovyException
    {
        try (InputStream stream = Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream( "test-scripts/simple-injection.groovy" ))
        {
            String scriptSrc = IOUtils.toString( stream );
            ScriptedThingOwner owner =
                    scriptEngine.parseScriptInstance( scriptSrc, ScriptedThingOwner.class, false );

            assertThat( owner, notNullValue() );
            assertThat( owner.getThing(), nullValue() );
        }
    }

    @Test
    public void testParsedScriptFields_InjectionDisabledByDefault()
            throws IOException, IndyGroovyException
    {
        try (InputStream stream = Thread.currentThread()
                                        .getContextClassLoader()
                                        .getResourceAsStream( "test-scripts/simple-injection.groovy" ))
        {
            String scriptSrc = IOUtils.toString( stream );
            ScriptedThingOwner owner =
                    scriptEngine.parseScriptInstance( scriptSrc, ScriptedThingOwner.class );

            assertThat( owner, notNullValue() );
            assertThat( owner.getThing(), nullValue() );
        }
    }

}
