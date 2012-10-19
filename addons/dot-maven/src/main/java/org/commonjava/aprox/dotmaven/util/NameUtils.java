package org.commonjava.aprox.dotmaven.util;

import static org.apache.commons.lang.StringUtils.join;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.util.logging.Logger;

public final class NameUtils
{

    private static final Logger logger = new Logger( NameUtils.class );

    public static final String SETTINGS_PATTERN = "\\/?settings-(deploy|group|repository)-(.+)\\.xml";

    private NameUtils()
    {
    }

    public static String makePath( final String[] parts, final int startingPos )
    {
        final String[] pathParts = new String[parts.length - startingPos];
        System.arraycopy( parts, startingPos, pathParts, 0, pathParts.length );
        return join( pathParts, "/" );
    }

    public static String appendPath( final String base, final String... parts )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( base );
        for ( final String part : parts )
        {
            if ( sb.charAt( sb.length() - 1 ) == '/' )
            {
                sb.setLength( sb.length() - 1 );
            }

            if ( !part.startsWith( "/" ) )
            {
                sb.append( "/" );
            }

            sb.append( part );
        }

        return sb.toString();
    }

    public static String trimLeadingSlash( final String childName )
    {
        String name = childName;
        if ( name.startsWith( "/" ) )
        {
            if ( name.length() > 1 )
            {
                name = name.substring( 1 );
            }
            else
            {
                name = null;
            }
        }

        return name;
    }

    public static String formatSettingsResourceName( final StoreKey key )
    {
        return formatSettingsResourceName( key.getType(), key.getName() );
    }

    public static String formatSettingsResourceName( final StoreType type, final String name )
    {
        return "settings-" + type.singularEndpointName() + "-" + name + ".xml";
    }

    public static boolean isSettingsResource( final String realPath )
    {
        return realPath.matches( SETTINGS_PATTERN );
    }

    public static StoreKey getStoreKey( final String settingsName )
    {
        final Matcher matcher = Pattern.compile( SETTINGS_PATTERN )
                                       .matcher( settingsName );
        if ( !matcher.matches() )
        {
            return null;
        }

        final String typePart = matcher.group( 1 );
        final String name = matcher.group( 2 );

        logger.info( "Type part of name is: '%s'", typePart );
        logger.info( "Store part of name is: '%s'", name );

        final StoreType type = StoreType.get( typePart );
        logger.info( "StoreType is: %s", type );

        if ( type == null )
        {
            return null;
        }

        return new StoreKey( type, name );
    }

}
