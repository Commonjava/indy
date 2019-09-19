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
package org.commonjava.indy.koji.inject;

import org.commonjava.atlas.maven.ident.ref.InvalidRefException;
import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KojiProjectRefStringKey2StringMapper
        implements TwoWayKey2StringMapper
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );
    private static final String SPLITTER = ":";
    @Override
    public Object getKeyMapping( String stringKey )
    {
        try
        {
            return SimpleProjectRef.parse( stringKey );
        }
        catch ( InvalidRefException e )
        {
            logger.warn(
                    "Koji meta cache JDBC store error: invalid groupId or artifact when deserializing from database: {}",
                    e.getMessage() );
        }

        return null;
    }

    @Override
    public boolean isSupportedType( Class<?> keyType )
    {
        return keyType == SimpleProjectRef.class;
    }

    @Override
    public String getStringMapping( Object key )
    {
        if ( key instanceof SimpleProjectRef )
        {
            return key.toString();
        }
        logger.warn( "Koji meta cache JDBC store error: Not supported key type {}",
                      key == null ? null : key.getClass() );
        return null;
    }
}
