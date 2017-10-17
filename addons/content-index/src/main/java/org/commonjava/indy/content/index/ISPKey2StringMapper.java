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
package org.commonjava.indy.content.index;

import org.infinispan.commons.util.Base64;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ISPKey2StringMapper
        implements TwoWayKey2StringMapper
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

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
            try (ByteArrayOutputStream b = new ByteArrayOutputStream())
            {
                try (ObjectOutputStream o = new ObjectOutputStream( b ))
                {
                    o.writeObject( key );
                }
                return Base64.encodeBytes( b.toByteArray() );
            }
            catch ( IOException e )
            {
                LOGGER.error( "Content Index JDBC store error: Not supported key type %s" );
            }

        }
        LOGGER.error( "Content Index JDBC store error: Not supported key type {}",
                      key == null ? null : key.getClass() );
        return null;
    }

    @Override
    public Object getKeyMapping( String stringKey )
    {
        byte[] bytes = Base64.decode( stringKey );
        try (ByteArrayInputStream b = new ByteArrayInputStream( bytes ))
        {
            try (ObjectInputStream o = new ObjectInputStream( b ))
            {
                LOGGER.trace( "deserializing key from content index jdbc store: {}", stringKey );

                return o.readObject();
            }
            catch ( IOException | ClassNotFoundException e )
            {
                LOGGER.error( "Content Index JDBC store unexpected error", e );
            }
        }
        catch ( IOException e )
        {
            LOGGER.error( "Content Index JDBC store unexpected error", e );
        }

        return null;
    }
}
