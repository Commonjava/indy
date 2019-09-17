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
package org.commonjava.indy.dotmaven.util;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

public class SettingsURIMatcher
    implements URIMatcher
{

    public static final String SETTINGS_TYPE_PATTERN = "\\/?settings(\\/(hosted|group|remote)(\\/settings-(.+).xml)?)?";

    private static final int TYPE_GRP = 2;

    private static final int NAME_GRP = 4;

    private final Matcher matcher;

    private final String uri;

    public SettingsURIMatcher( final String uri )
    {
        this.uri = uri;
        this.matcher = Pattern.compile( SETTINGS_TYPE_PATTERN )
                              .matcher( uri );
    }

    @Override
    public boolean matches()
    {
        return matcher.matches();
    }

    public boolean isSettingsRootResource()
    {
        return matches() && matcher.group( TYPE_GRP ) == null;
    }

    public boolean isSettingsTypeResource()
    {
        return matches() && matcher.group( TYPE_GRP ) != null;
    }

    public boolean isSettingsFileResource()
    {
        return matches() && matcher.group( NAME_GRP ) != null;
    }

    /* (non-Javadoc)
     * @see org.commonjava.indy.dotmaven.util.URIMatcher#getStoreType()
     */
    @Override
    public StoreType getStoreType()
    {
        if ( !matches() )
        {
            return null;
        }

        final String typePart = matcher.group( TYPE_GRP );
        if ( typePart == null )
        {
            return null;
        }

        final StoreType type = StoreType.get( typePart );
        return type;
    }

    /* (non-Javadoc)
     * @see org.commonjava.indy.dotmaven.util.URIMatcher#getStoreKey()
     */
    @Override
    public StoreKey getStoreKey()
    {
        final StoreType type = getStoreType();

        if ( type == null )
        {
            return null;
        }

        final String name = matcher.group( NAME_GRP );
        if ( isEmpty( name ) )
        {
            return null;
        }

        return new StoreKey( type, name );
    }

    /* (non-Javadoc)
     * @see org.commonjava.indy.dotmaven.util.URIMatcher#getURI()
     */
    @Override
    public String getURI()
    {
        return uri;
    }
}
