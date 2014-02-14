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

public final class StoreURIMatcher
    implements URIMatcher
{
    // @formatter:off
    private static final String STORE_TYPE_PATTERN = "\\/?storage(\\/(hosted|group|remote)(\\/([^/]+)(\\/(.+))?)?)?";
    // @formatter:on

    private static final int STORE_TYPE_GRP = 2;

    private static final int STORE_NAME_GRP = 4;

    private static final int STORE_PATH_GRP = 6;

    //    private final Logger logger = new Logger( getClass() );

    private final Matcher matcher;

    private final String uri;

    public StoreURIMatcher( final String uri )
    {
        this.uri = uri;
        this.matcher = Pattern.compile( STORE_TYPE_PATTERN )
                              .matcher( uri );
    }

    public StoreType getStoreFolderStoreType( final String uri )
    {
        if ( !matches() )
        {
            return null;
        }

        if ( hasStoreType() )
        {
            final String typePart = matcher.group( STORE_TYPE_GRP );
            //            logger.info( "Type part of name is: '%s'", typePart );

            final StoreType type = StoreType.get( typePart );
            //            logger.info( "StoreType is: %s", type );

            return type;
        }

        return null;
    }

    @Override
    public StoreKey getStoreKey()
    {
        if ( !matches() )
        {
            return null;
        }

        if ( !hasStoreName() )
        {
            return null;
        }

        final String typePart = matcher.group( STORE_TYPE_GRP );
        //        logger.info( "Type part of name is: '%s'", typePart );

        final StoreType type = StoreType.get( typePart );
        //        logger.info( "StoreType is: %s", type );

        if ( type == null )
        {
            return null;
        }

        final String name = matcher.group( STORE_NAME_GRP );
        //        logger.info( "Store part of name is: '%s'", name );

        return new StoreKey( type, name );
    }

    @Override
    public StoreType getStoreType()
    {
        if ( !matches() )
        {
            return null;
        }

        final String typePart = matcher.group( STORE_TYPE_GRP );
        //        logger.info( "Type part of name is: '%s'", typePart );

        if ( isEmpty( typePart ) )
        {
            return null;
        }

        final StoreType type = StoreType.get( typePart );
        //        logger.info( "StoreType is: %s", type );

        return type;
    }

    public String getStorePath()
    {
        if ( !matches() )
        {
            return null;
        }

        final String storePath = matcher.group( STORE_PATH_GRP );
        //        logger.info( "Path is: '%s'", storePath );
        return storePath;
    }

    @Override
    public boolean matches()
    {
        return matcher.matches();
    }

    public boolean hasStoreType()
    {
        return matches() && matcher.group( STORE_TYPE_GRP ) != null;
    }

    public boolean hasStoreName()
    {
        return matches() && matcher.group( STORE_NAME_GRP ) != null;
    }

    public boolean hasStorePath()
    {
        return matches() && matcher.group( STORE_PATH_GRP ) != null;
    }

    @Override
    public String getURI()
    {
        return uri;
    }

}
