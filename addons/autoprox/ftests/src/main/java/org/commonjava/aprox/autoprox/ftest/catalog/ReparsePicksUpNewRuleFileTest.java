package org.commonjava.aprox.autoprox.ftest.catalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.junit.Test;

public class ReparsePicksUpNewRuleFileTest
    extends AbstractAutoproxCatalogTest
{

    @Test
    public void writeRuleFileThenReparseCatalogAndVerifyRulePresence()
        throws Exception
    {
        final CatalogDTO catalog = module.getCatalog();

        assertThat( catalog.isEnabled(), equalTo( true ) );

        assertThat( catalog.getRules()
                           .isEmpty(), equalTo( true ) );

        final RuleDTO rule = getRule( "0001-simple-rule.groovy", "rules/simple-rule.groovy" );

        final File script = new File( fixture.getBootOptions()
                                             .getAproxHome(), "var/lib/aprox/data/autoprox/0001-simple-rule.groovy" );
        FileUtils.write( script, rule.getSpec() );

        module.reparseCatalog();

        final CatalogDTO resultCatalog = module.getCatalog();

        final List<RuleDTO> rules = resultCatalog.getRules();
        assertThat( rules.size(), equalTo( 1 ) );

        final RuleDTO dto = rules.get( 0 );
        assertThat( dto.getName(), equalTo( rule.getName() ) );
        assertThat( dto.getSpec(), equalTo( rule.getSpec() ) );
    }

}
