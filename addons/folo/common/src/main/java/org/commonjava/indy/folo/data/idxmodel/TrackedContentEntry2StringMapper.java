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

import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.AbstractIndyKey2StringMapper;

/**
 * The key2string mapper for {@link TrackedContentEntry} working for ISPN jdbc persistence. Note that this mapper only
 * cares about the fields that used in equals() method which follows {@link org.infinispan.persistence.keymappers.Key2StringMapper}
 * rules, so will ignore other fields
 */
public class TrackedContentEntry2StringMapper
        extends AbstractIndyKey2StringMapper<TrackedContentEntry>
{
    private final static String FIELD_SPLITTER = ";;";

    private final static String NULL_STR = "null";

    @Override
    public Object getKeyMapping( String stringKey )
    {
        String[] parts = stringKey.split( FIELD_SPLITTER );
        final TrackingKey trackingKey = NULL_STR.equals( parts[0] ) ? null : new TrackingKey( parts[0] );
        final String path = NULL_STR.equals( parts[1] ) ? null : parts[1];
        final StoreKey storeKey = NULL_STR.equals( parts[2] ) ? null : StoreKey.fromString( parts[2] );
        final AccessChannel channel = NULL_STR.equals( parts[3] ) ? null : AccessChannel.valueOf( parts[3] );
        final StoreEffect effect = NULL_STR.equals( parts[4] ) ? null : StoreEffect.valueOf( parts[4] );

        return new TrackedContentEntry( trackingKey, storeKey, channel, "", path, effect, 0L, "", "", "" );
    }

    @Override
    protected String getStringMappingFromInst( Object key )
    {
        if ( !( key instanceof TrackedContentEntry ) )
        {
            return null;
        }

        final TrackedContentEntry entry = (TrackedContentEntry) key;

        return ( entry.getTrackingKey() == null ? NULL_STR : entry.getTrackingKey().getId() ) + FIELD_SPLITTER
                + getNullOrString( entry.getPath() ) + FIELD_SPLITTER + getNullOrString( entry.getStoreKey() )
                + FIELD_SPLITTER + getNullOrString( entry.getAccessChannel() ) + FIELD_SPLITTER + getNullOrString(
                entry.getEffect() );
    }

    private String getNullOrString( final Object field )
    {
        return field == null ? NULL_STR : field.toString();
    }

    @Override
    protected Class<TrackedContentEntry> provideKeyClass()
    {
        return TrackedContentEntry.class;
    }
}
