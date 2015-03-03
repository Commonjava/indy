package org.commonjava.aprox.autoprox.ftest.catalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.junit.Test;

public class Catalog02Test
    extends AbstractAutoproxCatalogTest
{

    @Test
    public void createRuleAndVerifyListingReflectsIt()
        throws Exception
    {
        final CatalogDTO catalog = module.getCatalog();

        assertThat( catalog.isEnabled(), equalTo( true ) );

        assertThat( catalog.getRules()
                           .isEmpty(), equalTo( true ) );

        final RuleDTO rule = getRule( "0001-simple-rule", "rules/simple-rule.groovy" );
        final RuleDTO dto = module.storeRule( rule );

        assertThat( dto, notNullValue() );

        final CatalogDTO resultCatalog = module.getCatalog();

        final List<RuleDTO> rules = resultCatalog.getRules();
        assertThat( rules.size(), equalTo( 1 ) );
        assertThat( rules.get( 0 ), equalTo( dto ) );
    }

}
