/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.changelog.client;

import org.commonjava.indy.changelog.model.RepositoryChangeLog;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Arrays;
import java.util.List;

public class IndyRepoChangelogClientModule
        extends IndyClientModule
{
    public List<RepositoryChangeLog> getByStoreKey( final StoreKey key )
            throws IndyClientException
    {

        RepositoryChangeLog[] logs =
                http.get( UrlUtils.buildUrl( "repo/changelog", key.getPackageType(), key.getType().singularEndpointName(), key.getName() ), RepositoryChangeLog[].class );

        return Arrays.asList( logs );

    }

    public List<RepositoryChangeLog> getAll()
            throws IndyClientException
    {
        RepositoryChangeLog[] logs =
                http.get( UrlUtils.buildUrl( "repo/changelog/all" ), RepositoryChangeLog[].class );

        return Arrays.asList( logs );
    }
}
