package org.commonjava.aprox.util;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestUtils
{

    private static final Logger logger = LoggerFactory.getLogger( RequestUtils.class );

    private RequestUtils()
    {
    }

    private static final String DATE_HEADER_FMT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    public static String formatDateHeader( final long date )
    {
        return new SimpleDateFormat( DATE_HEADER_FMT ).format( new Date( date ) );
    }

    public static String formatDateHeader( final Date date )
    {
        return new SimpleDateFormat( DATE_HEADER_FMT ).format( date );
    }

    public static Map<String, String[]> parseQueryMap( final String query )
    {
        final Map<String, String[]> result = new HashMap<String, String[]>();

        if ( query != null )
        {
            final String[] qe = query.split( "&" );
            for ( final String entry : qe )
            {
                final int idx = entry.indexOf( '=' );
                String key;
                String value;
                if ( idx > 1 )
                {
                    key = entry.substring( 0, idx );
                    value = entry.substring( idx + 1 );
                }
                else
                {
                    key = entry;
                    value = "true";
                }

                final String[] values = result.get( key );
                if ( values == null )
                {
                    result.put( key, new String[] { value } );
                }
                else
                {
                    final String[] next = new String[values.length + 1];
                    System.arraycopy( values, 0, next, 0, values.length );
                    next[values.length] = value;

                    result.put( key, next );
                }
            }
        }

        return result;
    }

    public static boolean toBoolean( final String value, final boolean def )
    {
        if ( isEmpty( value ) )
        {
            return def;
        }

        return Boolean.parseBoolean( value );
    }

    public static boolean getBooleanParamWithDefault( final Map<String, String[]> params, final String key, final boolean def )
    {
        final String[] values = params.get( key );
        boolean val;
        if ( isEmpty( values[0] ) )
        {
            val = def;
        }
        else
        {
            val = Boolean.parseBoolean( values[0] );
        }

        logger.info( "Values of key: {} are: {}. Returning boolean-param-with-default value: {}", key, join( values, ", " ), val );
        return val;
    }

    public static String getFirstParameterValue( final Map<String, String[]> params, final String key )
    {
        final String[] values = params.get( key );
        return values == null || values.length < 1 ? null : values[0];
    }

    public static long getLongParamWithDefault( final Map<String, String[]> params, final String key, final long def )
    {
        final String[] values = params.get( key );
        long val;
        if ( values == null || values.length < 1 )
        {
            val = def;
        }
        else
        {
            val = Long.parseLong( values[0] );
        }

        logger.info( "Values of key: {} are: {}. Returning long-param-with-default value: {}", key, join( values, ", " ), val );
        return val;
    }

    public static String getStringParamWithDefault( final Map<String, String[]> params, final String key, final String def )
    {
        final String value = getFirstParameterValue( params, key );
        String val = value;
        if ( val == null || val.trim()
                               .length() < 1 )
        {
            val = def;
        }

        logger.info( "Value of key: {} is: {}. Returning string-param-with-default value: {}", key, value, val );
        return val;
    }

}
