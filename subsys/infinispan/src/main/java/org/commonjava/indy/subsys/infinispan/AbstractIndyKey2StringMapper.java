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
package org.commonjava.indy.subsys.infinispan;

import org.infinispan.commons.marshall.WrappedByteArray;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * A default Key2StringMapper for all indy ISPN cache keys which will be used for ISPN jdbc store. This class
 * aims to resolve some off-heap migration, which may use java ser/deser mechanism for storing key instances. It
 * will cause the deserialized instance wrapped in a ISPN WrappedByteArray instance. <br />
 * <strong>Note:</strong> Because this class uses reflection to initialize key instance, so all key class which extends
 * this should provide default non-arg public constructor for initialization.
 *
 * @param <T> - Key type to do this mapping
 */
public abstract class AbstractIndyKey2StringMapper<T>
        implements TwoWayKey2StringMapper
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

    @Override
    public boolean isSupportedType( Class<?> keyType )
    {
        return keyType == provideKeyClass() || keyType == WrappedByteArray.class;
    }

    @Override
    public String getStringMapping( Object key )
    {
        Class<T> typeClass = provideKeyClass();
        if ( typeClass == null )
        {
            return null;
        }
        Object keyObj = key;
        if ( keyObj != null )
        {
            if ( keyObj.getClass().isAssignableFrom( typeClass ) )
            {
                return getStringMappingFromInst( key );
            }
            else if ( keyObj instanceof WrappedByteArray )
            {
                try (ObjectInputStream objStream = new ObjectInputStream(
                        new ByteArrayInputStream( ( (WrappedByteArray) keyObj ).getBytes() ) ))
                {
                    T newKey = typeClass.newInstance();
                    if ( newKey instanceof Externalizable )
                    {
                        Externalizable deSerKey = (Externalizable) newKey;
                        deSerKey.readExternal( objStream );
                        return getStringMappingFromInst( newKey );
                    }
                    else
                    {
                        keyObj = objStream.readObject();
                        if ( keyObj.getClass().isAssignableFrom( provideKeyClass() ) )
                        {
                            return getStringMappingFromInst( keyObj );
                        }
                    }
                }
                catch ( IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e )
                {
                    LOGGER.error( "JDBC store error: Cannot deserialize key of WrappedByteArray, error is {}: {}",
                                  e.getClass(), e.getMessage() );
                }
            }
        }
        LOGGER.error( "JDBC store error: Not supported key type {}", keyObj == null ? null : keyObj.getClass() );
        return null;
    }

    /**
     * Sub class should implement this method to supply customized way to provide your string key from your key object.
     *
     * @param key - key object
     * @return - string type of the key
     */
    protected abstract String getStringMappingFromInst( Object key );

    /**
     * Sub class should implement this method to supply the Class type of your key object. Normally it is class object of your key type.
     *
     * @return
     */
    protected abstract Class<T> provideKeyClass();
}
