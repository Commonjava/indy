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

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Ignore;
import org.junit.Test;

public class RemoteRepoInValidUrlTest
        extends AbstractStoreManagementTest
{
    @Ignore( "Disabling validating decorator around StoreDataManager until we can be more certain it's correct and stable for all use cases" )
    @Test( expected = IndyClientException.class )
    public void run()
            throws Exception
    {
        final String INVALID_REPO = "invalid-repo";
        final String INVALID_URL = "this.is.not.valid.url";

        client.stores()
              .create( new RemoteRepository( INVALID_REPO, INVALID_URL ), "adding invalid test",
                       RemoteRepository.class );
    }

}
