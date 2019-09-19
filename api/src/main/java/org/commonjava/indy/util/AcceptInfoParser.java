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
package org.commonjava.indy.util;

import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * IN: application/indy-v1+json
 * OUT: {@link AcceptInfo} with raw equal to that given above, base = application/indy+json, version=v1
 * </pre>
 */
public class AcceptInfoParser
{

    public static final String APP_ID = "indy";

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
                logger.trace( "adding atomic addMetadata header: '{}'", accept );
                raw.add( accept );
            }
            else
            {
                logger.trace( "Adding split header values: '{}'", join( parts, "', '" ) );
                raw.addAll( Arrays.asList( parts ) );
            }
        }

        logger.trace( "Got raw ACCEPT header values:\n  {}", join( raw, "\n  " ) );

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

            logger.trace( "Cleaned up: {} to: {}", r, cleaned );

            final String appPrefix = "application/" + APP_ID + "-";

            logger.trace( "Checking for ACCEPT header starting with: '{}' and containing: '+' (header value is: '{}')",
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