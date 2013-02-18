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
    private static final String STORE_TYPE_PATTERN = "\\/?storage(\\/(deploys|groups|repositories)(\\/([^/]+)(\\/(.+))?)?)?";
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
