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

import org.commonjava.indy.autoprox.rest.dto.RuleDTO;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CreateRuleFailsWhenAutoProxDisabledTest
        extends AbstractAutoproxCatalogTest
{

    @Test( expected = IndyClientException.class )
    public void createRuleAndVerifyListingReflectsIt()
            throws Exception
    {
        final RuleDTO rule = getRule( "0001-simple-rule", "rules/simple-rule.groovy" );
        final RuleDTO dto = module.storeRule( rule );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/autoprox.conf", "[autoprox]\nenabled=false" );
    }
}
