package org.commonjava.aprox.autoprox.ftest.calc;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.autoprox.client.AutoProxCalculatorModule;
import org.commonjava.aprox.autoprox.client.AutoProxCatalogModule;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractAutoproxCalculatorTest
    extends AbstractAproxFunctionalTest
{

    protected AutoProxCalculatorModule module;

    @Before
    public void setup()
        throws Exception
    {
        installRule( "0001-simple-rule.groovy", "rules/simple-rule.groovy" );
    }

    protected RuleDTO installRule( final String named, final String ruleScriptResource )
        throws IOException, AproxClientException
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( ruleScriptResource );
        if ( resource == null )
        {
            Assert.fail( "Cannot find classpath resource: " + ruleScriptResource );
        }

        final String spec = IOUtils.toString( resource );

        return client.module( AutoProxCatalogModule.class )
                     .storeRule( new RuleDTO( named, spec ) );
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        module = new AutoProxCalculatorModule();
        return Arrays.<AproxClientModule> asList( module, new AutoProxCatalogModule() );
    }

}
