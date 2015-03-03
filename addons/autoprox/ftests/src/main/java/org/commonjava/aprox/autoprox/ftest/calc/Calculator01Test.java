package org.commonjava.aprox.autoprox.ftest.calc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class Calculator01Test
    extends AbstractAutoproxCalculatorTest
{

    @Test
    public void calculatedRemoteUsesUrlFromTestScript()
        throws Exception
    {
        final String name = "test";
        final AutoProxCalculation calculation = module.calculateRuleOutput( StoreType.remote, name );

        assertThat( calculation.getRuleName(), equalTo( "0001-simple-rule.groovy" ) );
        assertThat( calculation.getSupplementalStores()
                               .isEmpty(), equalTo( true ) );

        final RemoteRepository remote = (RemoteRepository) calculation.getStore();
        assertThat( remote.getUrl(), equalTo( "http://localhost:1000/target/" + name ) );
    }

}
