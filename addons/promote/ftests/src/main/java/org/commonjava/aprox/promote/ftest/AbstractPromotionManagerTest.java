package org.commonjava.aprox.promote.ftest;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.promote.client.AproxPromoteClientModule;
import org.junit.Before;

public class AbstractPromotionManagerTest
    extends AbstractAproxFunctionalTest
{

    protected final String first = "/first/path";

    protected final String second = "/second/path";

    protected HostedRepository source;

    protected HostedRepository target;

    @Before
    public void setupRepos()
        throws Exception
    {
        final AproxPromoteClientModule module = client.module( AproxPromoteClientModule.class );
        System.out.printf( "\n\n\n\nBASE-URL: %s\nPROMOTE-URL: %s\nRESUME-URL: %s\nROLLBACK-URL: %s\n\n\n\n",
                           client.getBaseUrl(), module.promoteUrl(), module.resumeUrl(), module.rollbackUrl() );

        source = new HostedRepository( "source" );
        client.stores()
              .create( source, HostedRepository.class );

        client.content()
              .store( source.getKey()
                            .getType(), source.getName(), first, new ByteArrayInputStream( "This is a test".getBytes() ) );
        client.content()
              .store( source.getKey()
                            .getType(), source.getName(), second,
                      new ByteArrayInputStream( "This is a test".getBytes() ) );

        target = new HostedRepository( "target" );
        client.stores()
              .create( target, HostedRepository.class );
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Arrays.<AproxClientModule> asList( new AproxPromoteClientModule() );
    }
}
