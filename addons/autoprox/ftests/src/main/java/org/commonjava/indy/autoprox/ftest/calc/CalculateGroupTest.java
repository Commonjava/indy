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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.commonjava.indy.autoprox.rest.dto.AutoProxCalculation;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

public class CalculateGroupTest
    extends AbstractAutoproxCalculatorTest
{

    @Test
    public void calculate()
        throws Exception
    {
        final String name = "test";
        final AutoProxCalculation calculation = module.calculateRuleOutput( StoreType.group, name );

        assertThat( calculation.getRuleName(), equalTo( "0001-simple-rule" ) );

        final List<ArtifactStore> supplemental = calculation.getSupplementalStores();
        assertThat( supplemental.size(), equalTo( 4 ) );

        final Group store = (Group) calculation.getStore();
        assertThat( store.getName(), equalTo( name ) );

        int idx = 0;
        ArtifactStore supp = supplemental.get( idx );
        assertThat( supp.getName(), equalTo( name ) );
        assertThat( supp instanceof HostedRepository, equalTo( true ) );

        final HostedRepository hosted = (HostedRepository) supp;
        assertThat( hosted.isAllowReleases(), equalTo( true ) );
        assertThat( hosted.isAllowSnapshots(), equalTo( true ) );

        idx++;
        supp = supplemental.get( idx );
        assertThat( supp.getName(), equalTo( name ) );
        assertThat( supp instanceof RemoteRepository, equalTo( true ) );

        final RemoteRepository remote = (RemoteRepository) supp;
        assertThat( remote.getUrl(), equalTo( "http://localhost:1000/target/" + name ) );

    }

}
