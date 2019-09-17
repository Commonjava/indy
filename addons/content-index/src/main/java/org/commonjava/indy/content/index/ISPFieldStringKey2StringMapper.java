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
package org.commonjava.indy.content.index;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.infinispan.commons.util.Base64;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ISPFieldStringKey2StringMapper
        implements TwoWayKey2StringMapper
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );
    
    private final static String FIELD_SPLITTER = ";;";

    @Override
    public boolean isSupportedType( Class<?> keyType )
    {
        return keyType == IndexedStorePath.class;
    }

    @Override
    public String getStringMapping( Object key )
    {
        if ( key instanceof IndexedStorePath )
        {
            StringBuilder builder = new StringBuilder();
            IndexedStorePath isp = (IndexedStorePath) key;
            if ( isp.getStoreType() == null || isp.getStoreName() == null || isp.getPath() == null )
            {
                LOGGER.error(
                        "Content Index JDBC store error: IndexedStorePath has invalid value for StoreType or StoreName or path" );
                return null;
            }
            builder.append( isp.getStoreType().toString() )
                   .append( FIELD_SPLITTER )
                   .append( isp.getStoreName() )
                   .append( FIELD_SPLITTER )
                   .append( isp.getPath() )
                   .append( FIELD_SPLITTER );
            if ( isp.getOriginStoreType() != null )
            {
                builder.append( isp.getOriginStoreType().toString() );
            }
            else
            {
                builder.append( "null" );
            }
            builder.append( FIELD_SPLITTER );
            if ( isp.getOriginStoreName() != null )
            {
                builder.append( isp.getOriginStoreName() );
            }
            else
            {
                builder.append( "null" );
            }
            return builder.toString();

        }
        LOGGER.error( "Content Index JDBC store error: Not supported key type {}",
                      key == null ? null : key.getClass() );
        return null;
    }

    @Override
    public Object getKeyMapping( String stringKey )
    {
        String[] parts = stringKey.split( FIELD_SPLITTER );

        StoreType type = StoreType.get( parts[0] );
        StoreKey key = new StoreKey( type, parts[1] );
        String path = parts[2];

        IndexedStorePath isp;
        if ( !parts[3].equals( "null" ) && !parts[4].equals( "null" ) )
        {
            StoreType originType = StoreType.get( parts[3] );
            StoreKey originKey = new StoreKey( originType, parts[4] );
            isp = new IndexedStorePath( key, originKey, path );
        }
        else
        {
            isp = new IndexedStorePath( key, path );
        }

        return isp;
    }
}
