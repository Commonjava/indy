/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.client.core.module;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;

public class IndyNfcClientModule
        extends IndyClientModule
{
    private static final String BASE_URL = "/nfc";

    // As nfc get endpoints has been removed in new nfc oom fixes, will make these two methods deprecated.
    @Deprecated
    public NotFoundCacheDTO getAllNfcContent()
            throws IndyClientException
    {
        return getHttp().get( BASE_URL, NotFoundCacheDTO.class );
    }

    @Deprecated
    public NotFoundCacheDTO getAllNfcContentInStore( final StoreType type, final String name )
            throws IndyClientException
    {
        return getHttp().get( BASE_URL + "/" + type.singularEndpointName() + "/" + name, NotFoundCacheDTO.class );
    }

    public void clearAll()
            throws IndyClientException
    {
        getHttp().delete( BASE_URL, 200 );
    }

    public void clearInStore( final StoreType type, final String name, final String path )
            throws IndyClientException
    {
        getHttp().delete( BASE_URL + "/" + type.singularEndpointName() + "/" + name + path );
    }

}
