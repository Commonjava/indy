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
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.infinispan.query.Transformer;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.io.IOException;

/**
 * A customized infinispan {@link Transformer} used for {@link TrackedContentEntry}
 * to support it to be used as infinispan cache key in indexing.
 */
public class TrackedContentEntryTransformer
        implements Transformer
{
    @Inject
    private IndyObjectMapper objectMapper;

    public TrackedContentEntryTransformer(){
        initMapper();
    }

    private void initMapper()
    {
        if ( objectMapper == null )
        {
            final CDI<Object> cdi = CDI.current();
            objectMapper = cdi.select( IndyObjectMapper.class ).get();
        }
    }

    @Override
    public Object fromString( String s )
    {
        try
        {
            return objectMapper.readValue( s, TrackedContentEntry.class );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e );
        }
    }

    @Override
    public String toString( Object customType )
    {
        if ( customType instanceof TrackedContentEntry )
        {
            try
            {
                return objectMapper.writeValueAsString( customType );
            }
            catch ( JsonProcessingException e )
            {
                throw new IllegalStateException( e );
            }
        }
        else
        {
            throw new IllegalArgumentException( "Expected customType to be a " + TrackedContentEntry.class.getName() );
        }
    }
}
