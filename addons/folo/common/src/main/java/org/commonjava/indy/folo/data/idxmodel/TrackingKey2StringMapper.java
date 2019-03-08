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
import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class TrackingKey2StringMapper
        implements TwoWayKey2StringMapper
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

    @Override
    public boolean isSupportedType( Class<?> keyType )
    {
        return keyType == TrackingKey.class || keyType == WrappedByteArray.class;
    }

    @Override
    public String getStringMapping( Object key )
    {
        Object keyObj = key;
        if ( keyObj instanceof TrackingKey )
        {
            TrackingKey tk = (TrackingKey) keyObj;
            return tk.getId();
        }
        else if ( keyObj instanceof WrappedByteArray )
        {
            try (ObjectInputStream objStream = new ObjectInputStream(
                    new ByteArrayInputStream( ( (WrappedByteArray) keyObj ).getBytes() ) ))
            {
                keyObj = objStream.readObject();
                if ( keyObj instanceof TrackingKey )
                {
                    return ( (TrackingKey) keyObj ).getId();
                }
            }
            catch ( IOException | ClassNotFoundException e )
            {
                LOGGER.error(
                        "Folo tracking JDBC store error: Cannot deserialize tracking key type {}, is using off-heap with unsupported type?",
                        keyObj == null ? null : keyObj.getClass() );
            }
        }
        LOGGER.error( "Folo tracking JDBC store error: Not supported key type {}", keyObj == null ? null : keyObj.getClass() );
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
