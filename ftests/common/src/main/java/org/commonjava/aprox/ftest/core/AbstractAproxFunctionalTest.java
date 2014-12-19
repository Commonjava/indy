package org.commonjava.aprox.ftest.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import org.commonjava.aprox.bind.vertx.boot.BootStatus;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.test.fixture.core.CoreVertxServerFixture;
import org.junit.After;
import org.junit.Before;

import com.fasterxml.jackson.databind.Module;

public abstract class AbstractAproxFunctionalTest
{
    private static final int NAME_LEN = 8;

    private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";

    protected Aprox client;

    private CoreVertxServerFixture fixture;

    @Before
    public void start()
        throws Throwable
    {
        fixture = newServerFixture();
        fixture.start();

        if ( !fixture.isStarted() )
        {
            final BootStatus status = fixture.getBootStatus();
            throw new IllegalStateException( "server fixture failed to boot.", status.getError() );
        }

        client =
            new Aprox( fixture.getUrl(), new AproxObjectMapper( getAdditionalMapperModules() ),
                       getAdditionalClientModules() ).connect();
    }

    @After
    public void stop()
    {
        fixture.stop();
    }

    protected CoreVertxServerFixture newServerFixture()
    {
        return new CoreVertxServerFixture();
    }

    protected Collection<Module> getAdditionalMapperModules()
    {
        return Collections.emptySet();
    }

    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Collections.emptySet();
    }

    protected String newName()
    {
        final Random rand = new Random();
        final StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < NAME_LEN; i++ )
        {
            sb.append( NAME_CHARS.charAt( ( Math.abs( rand.nextInt() ) % ( NAME_CHARS.length() - 1 ) ) ) );
        }

        return sb.toString();
    }

    protected String newUrl()
    {
        return String.format( "http://%s.com/", newName() );
    }

}
