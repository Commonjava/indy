/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.folo.data.idxmodel;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.folo.model.TrackingKey;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackingKey2StringMapper implements TwoWayKey2StringMapper
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

    @Override
    public boolean isSupportedType( Class<?> keyType )
    {
        return keyType == TrackingKey.class;
    }

    @Override
    public String getStringMapping( Object key )
    {
        if ( key instanceof TrackingKey )
        {
            TrackingKey tk = (TrackingKey) key;
            return tk.getId();

        }
        LOGGER.error( "Folo tracking JDBC store error: Not supported key type {}",
                      key == null ? null : key.getClass() );
        return null;
    }

    @Override
    public Object getKeyMapping( String stringKey )
    {
        if ( StringUtils.isNotEmpty( stringKey ) )
        {
            return new TrackingKey( stringKey );
        }
        else
        {
            LOGGER.error( "Folo tracking JDBC store error: an empty store key from store" );
            return null;
        }
    }
}
