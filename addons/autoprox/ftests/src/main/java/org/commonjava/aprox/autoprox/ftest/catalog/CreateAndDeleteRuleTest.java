package org.commonjava.aprox.autoprox.ftest.catalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.junit.Test;

public class CreateAndDeleteRuleTest
    extends AbstractAutoproxCatalogTest
{

    @Test
    public void createRuleDeleteAndVerifyMissing()
        throws Exception
    {
        final CatalogDTO catalog = module.getCatalog();

        assertThat( catalog.isEnabled(), equalTo( true ) );

        assertThat( catalog.getRules()
                           .isEmpty(), equalTo( true ) );

        final RuleDTO rule = getRule( "0001-simple-rule", "rules/simple-rule.groovy" );
        RuleDTO dto = module.storeRule( rule );

        assertThat( dto, notNullValue() );
        assertThat( dto, equalTo( rule ) );

        module.deleteRuleNamed( dto.getName() );

        dto = module.getRuleNamed( dto.getName() );

        assertThat( dto, nullValue() );
    }

}
