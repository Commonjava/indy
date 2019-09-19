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
package org.commonjava.indy.content.browse.client;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.content.browse.model.ContentBrowseResult;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

public class IndyContentBrowseClientModule
        extends IndyClientModule
{
    public ContentBrowseResult getContentList( final StoreKey key, final String path )
            throws IndyClientException
    {
        return http.get(
                UrlUtils.buildUrl( "browse", key.getPackageType(), key.getType().singularEndpointName(), key.getName(),
                                   path ), ContentBrowseResult.class );
    }

    public ContentBrowseResult getContentList( final String packageType, final StoreType type, final String name,
                                               final String path )
            throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "browse", packageType, type.singularEndpointName(), name, path ),
                         ContentBrowseResult.class );
    }
}
