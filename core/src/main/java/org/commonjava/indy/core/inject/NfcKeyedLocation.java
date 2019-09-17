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
package org.commonjava.indy.core.inject;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;

import java.util.Map;

/**
 * Created by ruhan on 11/29/17.
 */
public class NfcKeyedLocation implements KeyedLocation
{
    private StoreKey storeKey;

    public NfcKeyedLocation( StoreKey storeKey )
    {
        this.storeKey = storeKey;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;

        NfcKeyedLocation that = (NfcKeyedLocation) o;

        return storeKey.equals( that.storeKey );

    }

    @Override
    public int hashCode()
    {
        return storeKey.hashCode();
    }

    @Override
    public StoreKey getKey()
    {
        return storeKey;
    }

    @Override
    public boolean allowsDownloading()
    {
        return false;
    }

    @Override
    public boolean allowsPublishing()
    {
        return false;
    }

    @Override
    public boolean allowsStoring()
    {
        return false;
    }

    @Override
    public boolean allowsSnapshots()
    {
        return false;
    }

    @Override
    public boolean allowsReleases()
    {
        return false;
    }

    @Override
    public boolean allowsDeletion()
    {
        return false;
    }

    @Override
    public String getUri()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return null;
    }

    @Override
    public <T> T getAttribute( String s, Class<T> aClass )
    {
        return null;
    }

    @Override
    public <T> T getAttribute( String s, Class<T> aClass, T t )
    {
        return null;
    }

    @Override
    public Object removeAttribute( String s )
    {
        return null;
    }

    @Override
    public Object setAttribute( String s, Object o )
    {
        return null;
    }
}
