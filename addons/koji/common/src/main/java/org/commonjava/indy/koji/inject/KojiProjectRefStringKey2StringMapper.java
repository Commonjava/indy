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
package org.commonjava.indy.koji.inject;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.koji.content.KojiMavenMetadataProvider;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
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
        String[] parts = stringKey.split( SPLITTER );

        final String groupId = parts[0];
        final String artifactId = parts[1];

        if ( StringUtils.isNotBlank( groupId ) && StringUtils.isNotBlank( artifactId ) )
        {
            return KojiMavenMetadataProvider.newProjectRef( groupId, artifactId );
        }

        logger.warn(
                "Koji meta cache JDBC store error: invalid groupId {} or artifact {} when deserializing from database",
                groupId, artifactId );

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
            StringBuilder builder = new StringBuilder();
            SimpleProjectRef projectRef = (SimpleProjectRef) key;
            if ( projectRef.getGroupId() == null || projectRef.getArtifactId() == null)
            {
                logger.warn(
                        "Koji meta cache JDBC store error: ProjectRef has invalid value for groupId or artifactId" );
                return null;
            }
            builder.append( projectRef.getGroupId() )
                   .append( SPLITTER )
                   .append( projectRef.getArtifactId() );
            return builder.toString();

        }
        logger.warn( "Koji meta cache JDBC store error: Not supported key type {}",
                      key == null ? null : key.getClass() );
        return null;
    }
}
