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

import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.junit.Assert.assertTrue;

public class RemoteRepoValidUrlTest
        extends AbstractStoreManagementTest
{
    @Test
    public void run()
            throws Exception
    {
        final String VALID_REPO = "valid";
        final String VALID_URL = "http://www.foo.com";

        client.stores().create( new RemoteRepository( VALID_REPO, VALID_URL ),
                                "adding valid test", RemoteRepository.class );

        assertTrue( client.stores().exists( remote, VALID_REPO ) );
    }

}
