package org.commonjava.aprox.rest.util;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.util.logging.Logger;

public final class RequestUtils
{

    private static final Logger logger = new Logger( RequestUtils.class );

    private RequestUtils()
    {
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

        logger.info( "Values of key: %s are: %s. Returning boolean-param-with-default value: %s", key, join( values, ", " ), val );
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

        logger.info( "Values of key: %s are: %s. Returning long-param-with-default value: %s", key, join( values, ", " ), val );
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

        logger.info( "Value of key: %s is: %s. Returning string-param-with-default value: %s", key, value, val );
        return val;
    }

}
