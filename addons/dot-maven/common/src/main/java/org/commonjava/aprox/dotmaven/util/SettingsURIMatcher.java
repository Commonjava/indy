/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.dotmaven.util;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

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
     * @see org.commonjava.aprox.dotmaven.util.URIMatcher#getStoreType()
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
     * @see org.commonjava.aprox.dotmaven.util.URIMatcher#getStoreKey()
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
     * @see org.commonjava.aprox.dotmaven.util.URIMatcher#getURI()
     */
    @Override
    public String getURI()
    {
        return uri;
    }
}
