package org.commonjava.aprox.ftest.core.urls;

import static org.commonjava.aprox.model.core.StoreType.group;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientHttp;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.ftest.core.fixture.AproxRawHttpModule;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class AbstractCoreUrlsTest
    extends AbstractAproxFunctionalTest
{

    protected static final String STORE = "test";

    protected static final String PUBLIC = "public";

    @Rule
    public TestName name = new TestName();

    protected Thread newThread( final String named, final Runnable runnable )
    {
        final Thread t = new Thread( runnable );
        t.setDaemon( true );
        t.setName( name.getMethodName() + " :: " + named );

        return t;
    }

    @Before
    public void before()
        throws Exception
    {
        if ( !createStandardTestStructures() )
        {
            return;
        }

        final String changelog = "Create test structures";

        final HostedRepository hosted =
            this.client.stores()
                       .create( new HostedRepository( STORE ), changelog, HostedRepository.class );

        Group g;
        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            System.out.println( "Loading pre-existing public group." );
            g = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            System.out.println( "Creating new group 'public'" );
            g = client.stores()
                      .create( new Group( PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

    protected AproxClientHttp getHttp()
        throws AproxClientException
    {
        return client.module( AproxRawHttpModule.class )
                     .getHttp();
    }

    protected boolean createStandardTestStructures()
    {
        return true;
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Collections.<AproxClientModule> singleton( new AproxRawHttpModule() );
    }

}
