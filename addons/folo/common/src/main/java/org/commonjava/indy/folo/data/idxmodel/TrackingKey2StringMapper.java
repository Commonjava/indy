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

import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.infinispan.AbstractIndyKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackingKey2StringMapper
        extends AbstractIndyKey2StringMapper<TrackingKey>
{
    private final Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

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

    @Override
    protected String getStringMappingFromInst( Object key )
    {
        if ( !( key instanceof TrackingKey ) )
        {
            return null;
        }
        return ( (TrackingKey) key ).getId();
    }

    @Override
    protected Class<TrackingKey> provideKeyClass()
    {
        return TrackingKey.class;
    }
}
