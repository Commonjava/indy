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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.model.core.StoreKey;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleCacheKey2StringMapper
        implements TwoWayKey2StringMapper
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private final static String FIELD_SPLITTER = ";;";

    @Override
    public Object getKeyMapping( String stringKey )
    {
        final String[] parts = stringKey.split( FIELD_SPLITTER );

        final StoreKey storeKey = StoreKey.fromString( parts[0] );
        final String type = parts[1];
        final String name = parts[2];

        return new ScheduleKey( storeKey, type, name );
    }

    @Override
    public boolean isSupportedType( Class<?> keyType )
    {
        return keyType == ScheduleKey.class;
    }

    @Override
    public String getStringMapping( Object key )
    {
        if ( key instanceof ScheduleKey )
        {
            final StringBuilder builder = new StringBuilder();
            final ScheduleKey scheduleKey = (ScheduleKey) key;
            if ( scheduleKey.getStoreKey() == null || scheduleKey.getName() == null || scheduleKey.getType() == null )
            {
                logger.error(
                        "ScheduleManager cache JDBC store error: ScheduleKey has invalid value for StoreKey or name or type" );
            }
            builder.append( scheduleKey.getStoreKey().toString() )
                   .append( FIELD_SPLITTER )
                   .append( scheduleKey.getType() )
                   .append( FIELD_SPLITTER )
                   .append( scheduleKey.getName() )
                   .append( FIELD_SPLITTER );
            return builder.toString();
        }

        logger.error( "ScheduleManager cache JDBC store error: Not supported key type {}",
                      key == null ? null : key.getClass() );
        return null;
    }

}
