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
package org.commonjava.indy.changelog.client;

import org.commonjava.auditquery.history.ChangeEvent;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IndyRepoChangelogClientModule
        extends IndyClientModule
{
    public List<ChangeEvent> getByStoreKey( final StoreKey key )
            throws IndyClientException
    {

        ChangeEvent[] logs =
                http.get( UrlUtils.buildUrl( "repo/changelog", key.getPackageType(), key.getType().singularEndpointName(), key.getName() ), ChangeEvent[].class );

        if ( logs != null && logs.length > 0 )
        {
            return Arrays.asList( logs );
        }
        else
        {
            return Collections.emptyList();
        }
    }

    public List<ChangeEvent> getAll()
            throws IndyClientException
    {
        ChangeEvent[] logs =
                http.get( UrlUtils.buildUrl( "repo/changelog/all" ), ChangeEvent[].class );

        if ( logs != null && logs.length > 0 )
        {
            return Arrays.asList( logs );
        }
        else
        {
            return Collections.emptyList();
        }
    }
}
