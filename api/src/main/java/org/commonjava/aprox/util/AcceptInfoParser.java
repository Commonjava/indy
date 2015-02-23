package org.commonjava.aprox.util;

import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcceptInfoParser
{

    public static final String APP_ID = "aprox";

    public static final String DEFAULT_VERSION = "v1";

    public List<AcceptInfo> parse( final String... accepts )
    {
        return parse( Arrays.asList( accepts ) );
    }

    public List<AcceptInfo> parse( final Collection<String> accepts )
    {
        final Logger logger = LoggerFactory.getLogger( AcceptInfo.class );

        final List<String> raw = new ArrayList<String>();
        for ( final String accept : accepts )
        {
            final String[] parts = accept.split( "\\s*,\\s*" );
            if ( parts.length == 1 )
            {
                logger.info( "adding atomic accept header: '{}'", accept );
                raw.add( accept );
            }
            else
            {
                logger.info( "Adding split header values: '{}'", join( parts, "', '" ) );
                raw.addAll( Arrays.asList( parts ) );
            }
        }

        logger.info( "Got raw ACCEPT header values:\n  {}", join( raw, "\n  " ) );

        if ( raw == null || raw.isEmpty() )
        {
            return Collections.singletonList( new AcceptInfo( AcceptInfo.ACCEPT_ANY, AcceptInfo.ACCEPT_ANY,
                                                              DEFAULT_VERSION ) );
        }

        final List<AcceptInfo> acceptInfos = new ArrayList<AcceptInfo>();
        for ( final String r : raw )
        {
            String cleaned = r.toLowerCase();
            final int qIdx = cleaned.indexOf( ';' );
            if ( qIdx > -1 )
            {
                // FIXME: We shouldn't discard quality suffix...
                cleaned = cleaned.substring( 0, qIdx );
            }

            logger.info( "Cleaned up: {} to: {}", r, cleaned );

            final String appPrefix = "application/" + APP_ID + "-";

            logger.info( "Checking for ACCEPT header starting with: '{}' and containing: '+' (header value is: '{}')",
                         appPrefix, cleaned );
            if ( cleaned.startsWith( appPrefix ) && cleaned.contains( "+" ) )
            {
                final String[] acceptParts = cleaned.substring( appPrefix.length() )
                                                    .split( "\\+" );

                acceptInfos.add( new AcceptInfo( cleaned, "application/" + acceptParts[1], acceptParts[0] ) );
            }
            else
            {
                acceptInfos.add( new AcceptInfo( cleaned, cleaned, DEFAULT_VERSION ) );
            }
        }

        return acceptInfos;
    }

    public List<AcceptInfo> parse( final Enumeration<String> accepts )
    {
        return parse( Collections.list( accepts ) );
    }

    public String getDefaultVersion()
    {
        return DEFAULT_VERSION;
    }

}