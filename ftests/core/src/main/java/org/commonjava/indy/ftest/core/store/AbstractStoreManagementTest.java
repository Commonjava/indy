/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.ftest.core.store;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;

public class AbstractStoreManagementTest
    extends AbstractIndyFunctionalTest
{

    protected final void checkListing( final StoreListingDTO<? extends ArtifactStore> dto,
                                     final Set<ArtifactStore> expected, final List<Set<ArtifactStore>> banned )
    {
        final List<? extends ArtifactStore> stores = dto.getItems();

        for ( final ArtifactStore store : expected )
        {
            assertThat( store.getKey() + " should be present in:\n  " + join( keys( stores ), "\n  " ),
                        stores.contains( store ),
                        equalTo( true ) );
        }

        for ( final Set<ArtifactStore> bannedSet : banned )
        {
            for ( final ArtifactStore store : bannedSet )
            {
                assertThat( store.getKey() + " should NOT be present in:\n  " + join( keys( stores ), "\n  " ),
                            stores.contains( store ),
                            equalTo( false ) );
            }
        }
    }

    protected List<StoreKey> keys( final List<? extends ArtifactStore> stores )
    {
        final List<StoreKey> keys = new ArrayList<>();
        for ( final ArtifactStore store : stores )
        {
            keys.add( store.getKey() );
        }

        return keys;
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", readTestResource( "default-test-main.conf" ) );
    }

}
