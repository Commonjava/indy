/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.autoprox.ftest.calc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class CalculateRemoteRepoTest
    extends AbstractAutoproxCalculatorTest
{

    @Test
    public void calculate()
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
