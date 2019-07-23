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
package org.commonjava.indy.model.util;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpUtils
{

    private static final Logger logger = LoggerFactory.getLogger( HttpUtils.class );

    private HttpUtils()
    {
    }

    private static final String DATE_HEADER_FMT = "EEE, dd MMM yyyy HH:mm:ss";

    private static final String GMT_SUFFIX = " GMT";

    public static String formatDateHeader( final long date )
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( new Date( date ) );
        cal.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        return new SimpleDateFormat( DATE_HEADER_FMT ).format( cal.getTime() ) + GMT_SUFFIX;
    }

    public static String formatDateHeader( final Date date )
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        return new SimpleDateFormat( DATE_HEADER_FMT ).format( date ) + GMT_SUFFIX;
    }

    public static Date parseDateHeader( final String date )
        throws ParseException
    {
        return new SimpleDateFormat( DATE_HEADER_FMT ).parse( date );
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
        if ( values == null || values.length < 1 || isEmpty( values[0] ) )
        {
            val = def;
        }
        else
        {
            val = Boolean.parseBoolean( values[0] );
        }

        logger.debug( "Values of key: {} are: {}. Returning boolean-param-with-default value: {}", key,
                      joinString( ", ", values ), val );
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

        logger.debug( "Values of key: {} are: {}. Returning long-param-with-default value: {}", key,
                      joinString( ", ", values ), val );
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

        logger.debug( "Value of key: {} is: {}. Returning string-param-with-default value: {}", key, value, val );
        return val;
    }

    private static Object joinString( final String joint, final Object[] values )
    {
        return new Object()
        {
            @Override
            public String toString()
            {
                return values == null || values.length < 1 ? "NONE" : StringUtils.join( values, joint );
            }
        };
    }

}
