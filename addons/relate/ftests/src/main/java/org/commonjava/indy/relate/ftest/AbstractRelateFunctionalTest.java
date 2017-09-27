package org.commonjava.indy.relate.ftest;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;

import java.io.IOException;

/**
 * Created by jdcasey on 9/27/17.
 */
public abstract class AbstractRelateFunctionalTest
        extends AbstractIndyFunctionalTest
{
    @Override
    protected void initTestConfig( final CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "conf.d/relate.conf", "[relate]\nenabled=true" );
    }
}
