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
package org.commonjava.indy.client.core.module;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.commonjava.indy.model.core.dto.NotFoundCacheInfoDTO;

import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.commons.lang.StringUtils.isBlank;

public class IndyNfcClientModule
        extends IndyClientModule
{
    private static final String BASE_URL = "/nfc";

    public NotFoundCacheDTO getAllNfcContent()
            throws IndyClientException
    {
        return getHttp().get( BASE_URL, NotFoundCacheDTO.class );
    }

    public NotFoundCacheDTO getAllNfcContentInStore( final StoreType type, final String name )
            throws IndyClientException
    {
        return getHttp().get( BASE_URL + "/" + type.singularEndpointName() + "/" + name, NotFoundCacheDTO.class );
    }

    public NotFoundCacheDTO getAllNfcContent( Integer pageIndex, Integer pageSize )
                    throws IndyClientException
    {
        return getPagedNfcContent( BASE_URL, pageIndex, pageSize );
    }

    public NotFoundCacheDTO getAllNfcContentInStore( final StoreType type, final String name, Integer pageIndex, Integer pageSize  )
                    throws IndyClientException
    {
        return getPagedNfcContent( BASE_URL + "/" + type.singularEndpointName() + "/" + name, pageIndex, pageSize );
    }

    private NotFoundCacheDTO getPagedNfcContent( String baseUrl, Integer pageIndex, Integer pageSize  )
                    throws IndyClientException
    {
        IndyClientHttp clientHttp = getHttp();
        HttpGet req = clientHttp.newJsonGet( baseUrl );
        URI uri = null;
        try
        {
            uri = new URIBuilder( req.getURI() )
                            .addParameter( "pageIndex", pageIndex.toString() )
                            .addParameter( "pageSize", pageSize.toString() )
                            .build();
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }
        return clientHttp.get( uri.toString(), NotFoundCacheDTO.class );
    }

    public void clearAll()
            throws IndyClientException
    {
        getHttp().delete( BASE_URL, 200 );
    }

    public void clearInStore( final StoreType type, final String name, final String path )
            throws IndyClientException
    {
        if ( isBlank(path) )
        {
            getHttp().delete( BASE_URL + "/" + type.singularEndpointName() + "/" + name, 200 );
        }
        else
        {
            getHttp().delete( BASE_URL + "/" + type.singularEndpointName() + "/" + name + "/" + path, 200 );
        }
    }

    public NotFoundCacheInfoDTO getInfo( StoreKey key ) throws IndyClientException
    {
        String name = key.getName();
        StoreType type = key.getType();
        return getHttp().get( BASE_URL + "/" + key.getPackageType() + "/" + type.singularEndpointName()
                                              + "/" + name + "/info", NotFoundCacheInfoDTO.class );
    }

    public NotFoundCacheInfoDTO getInfo( ) throws IndyClientException
    {
        return getHttp().get( BASE_URL + "/info", NotFoundCacheInfoDTO.class );
    }

}
