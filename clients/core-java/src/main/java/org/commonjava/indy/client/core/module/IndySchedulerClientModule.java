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

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.core.expire.Expiration;
import org.commonjava.indy.core.expire.ExpirationSet;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 1/4/16.
 */
public class IndySchedulerClientModule
        extends IndyClientModule
{

    private static final String DISABLE_TIMEOUT = "disable-timeout";

    private static final String BY_STORE_BASE = "admin/schedule/store";

    private static final String ALL_STORES = "all";

    public Date getStoreDisableTimeout( StoreType type, String name )
            throws IndyClientException
    {
        return getStoreDisableTimeout( new StoreKey( type, name ) );
    }

    public Date getStoreDisableTimeout( StoreKey key )
            throws IndyClientException
    {
        Expiration exp = getHttp().get(
                UrlUtils.buildUrl( BY_STORE_BASE, key.getType().singularEndpointName(), key.getName(),
                                   DISABLE_TIMEOUT ), Expiration.class );

        if ( exp == null )
        {
            return null;
        }

        return exp.getExpiration();
    }

    public Map<StoreKey, Date> getDisabledStoreTimeouts()
            throws IndyClientException
    {
        ExpirationSet expSet =
                getHttp().get( UrlUtils.buildUrl( BY_STORE_BASE, ALL_STORES, DISABLE_TIMEOUT ), ExpirationSet.class );

        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( expSet == null )
        {
            logger.debug( "No disabled stores!" );
            return Collections.emptyMap();
        }

        Map<StoreKey, Date> result = new HashMap<>();
        expSet.forEach( ( exp ) -> {
            logger.debug( "Mapping expiration for group: {} (parts: {})", exp.getGroup(),
                          Arrays.asList( exp.getGroup().split( "\\s*#\\s*" ) ) );

            String[] parts = exp.getGroup().split( "\\s*#\\s*" );
            if ( parts.length < 2 )
            {
                logger.warn( "Skipping invalid store-disabled timeout group: '{}'", exp.getGroup() );
            }
            else
            {
                StoreKey key = StoreKey.fromString( parts[0] );
                logger.debug( "{} -> {}", key, exp.getExpiration() );

                result.put( key, exp.getExpiration() );
            }
        } );

        return result;
    }
}
