package org.commonjava.aprox.autoprox.ftest.calc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class Calculator02Test
    extends AbstractAutoproxCalculatorTest
{

    @Test
    public void calculatedRemoteUsesUrlFromTestScript()
        throws Exception
    {
        final String name = "test";
        final AutoProxCalculation calculation = module.calculateRuleOutput( StoreType.hosted, name );

        assertThat( calculation.getRuleName(), equalTo( "0001-simple-rule.groovy" ) );
        assertThat( calculation.getSupplementalStores()
                               .isEmpty(), equalTo( true ) );

        final HostedRepository remote = (HostedRepository) calculation.getStore();
        assertThat( remote.isAllowReleases(), equalTo( true ) );
        assertThat( remote.isAllowSnapshots(), equalTo( true ) );
    }

}
