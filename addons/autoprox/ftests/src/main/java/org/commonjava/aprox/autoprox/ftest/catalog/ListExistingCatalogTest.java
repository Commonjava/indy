package org.commonjava.aprox.autoprox.ftest.catalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.autoprox.rest.dto.CatalogDTO;
import org.junit.Test;

public class ListExistingCatalogTest
    extends AbstractAutoproxCatalogTest
{

    @Test
    public void listDefaultCatalog()
        throws Exception
    {
        final CatalogDTO catalog = module.getCatalog();

        assertThat( catalog.isEnabled(), equalTo( true ) );
    }

}
