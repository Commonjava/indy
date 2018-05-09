/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.autoprox.ftest.catalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.indy.autoprox.rest.dto.CatalogDTO;
import org.junit.Test;

public class ListExistingCatalogTest
    extends AbstractAutoproxCatalogTest
{

    @Test
    public void listDefaultCatalog()
        throws Exception
    {
        try
        {
            final CatalogDTO catalog = module.getCatalog();

            assertThat( catalog.isEnabled(), equalTo( true ) );
        }
        catch ( Exception e )
        {
            logger.error( "Test error", e );

            throw e;
        }
    }

}
