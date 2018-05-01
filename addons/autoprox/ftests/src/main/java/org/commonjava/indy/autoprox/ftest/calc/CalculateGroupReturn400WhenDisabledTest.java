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
package org.commonjava.indy.autoprox.ftest.calc;

import org.commonjava.indy.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertThat;

public class CalculateGroupReturn400WhenDisabledTest
    extends AbstractAutoproxCalculatorTest
{

    protected boolean doRuleInit()
    {
        return false;
    }

    @Test( expected = IndyClientException.class )
    public void calculate()
        throws Exception
    {
        final String name = "test";
        final AutoProxCalculation calculation = module.calculateRuleOutput( StoreType.group, name );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/autoprox.conf", "[autoprox]\nenabled=false" );
    }
}
