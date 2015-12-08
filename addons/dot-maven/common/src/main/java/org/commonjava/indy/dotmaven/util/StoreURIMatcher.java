/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

public final class StoreURIMatcher
    implements URIMatcher
{
    // @formatter:off
    private static final String STORE_TYPE_PATTERN = "\\/?storage(\\/(hosted|group|remote)(\\/([^/]+)(\\/(.+))?)?)?";
    // @formatter:on

    private static final int STORE_TYPE_GRP = 2;

    private static final int STORE_NAME_GRP = 4;

    private static final int STORE_PATH_GRP = 6;

    //    private final Logger logger = LoggerFactory.getLogger( getClass() );

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
            //            logger.info( "Type part of name is: '{}'", typePart );

            final StoreType type = StoreType.get( typePart );
            //            logger.info( "StoreType is: {}", type );

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
        //        logger.info( "Type part of name is: '{}'", typePart );

        final StoreType type = StoreType.get( typePart );
        //        logger.info( "StoreType is: {}", type );

        if ( type == null )
        {
            return null;
        }

        final String name = matcher.group( STORE_NAME_GRP );
        //        logger.info( "Store part of name is: '{}'", name );

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
        //        logger.info( "Type part of name is: '{}'", typePart );

        if ( isEmpty( typePart ) )
        {
            return null;
        }

        final StoreType type = StoreType.get( typePart );
        //        logger.info( "StoreType is: {}", type );

        return type;
    }

    public String getStorePath()
    {
        if ( !matches() )
        {
            return null;
        }

        final String storePath = matcher.group( STORE_PATH_GRP );
        //        logger.info( "Path is: '{}'", storePath );
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
