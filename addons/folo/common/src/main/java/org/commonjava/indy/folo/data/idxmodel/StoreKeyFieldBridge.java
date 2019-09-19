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
package org.commonjava.indy.folo.data.idxmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.hibernate.search.bridge.TwoWayStringBridge;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.IOException;

/**
 * A FieldBridge used for {@link TrackedContentEntry} storeKey field when do indexing by hibernate search.
 * Used json as the ser/de-ser way
 */
public class StoreKeyFieldBridge
        implements TwoWayStringBridge
{
    @Inject
    private ObjectMapper objMapper;

    public StoreKeyFieldBridge()
    {
        initMapper();
    }

    private void initMapper()
    {
        if ( objMapper == null )
        {
            final CDI<Object> cdi = CDI.current();
            objMapper = cdi.select( IndyObjectMapper.class ).get();
        }
    }

    @Override
    public Object stringToObject( String stringValue )
    {
        if ( "".equals( stringValue ) )
        {
            return null;
        }
        else
        {
            try
            {
                return objMapper.readValue( stringValue, TrackedContentEntry.class );
            }
            catch ( IOException e )
            {
                throw new IllegalStateException( e );
            }
        }
    }

    @Override
    public String objectToString( Object object )
    {
        if ( object instanceof StoreKey )
        {
            try
            {
                return objMapper.writeValueAsString( object );
            }
            catch ( JsonProcessingException e )
            {
                throw new IllegalStateException( e );
            }
        }
        return "";
    }

}