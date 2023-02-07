/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.db.service;

import org.apache.commons.io.IOUtils;
import org.commonjava.test.http.expect.ExpectationHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

class Utils
{
    static String readResource( final String resourcePath )
            throws IOException
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource( resourcePath );
        if ( url != null )
        {
            return IOUtils.toString( url.openStream(), Charset.defaultCharset() );
        }
        throw new IOException( String.format( "File not exists: %s", resourcePath ) );
    }

    static ExpectationHandler queryWithParamHandler( final Map<String, Object> queryParams, final String listingRes )
    {
        return ( req, res ) -> {
            if ( !isParamEquals( queryParams, req ) )
            {
                res.setStatus( SC_NOT_FOUND );
                return;
            }
            res.setStatus( SC_OK );
            final String json = Utils.readResource( listingRes );
            res.getWriter().write( json );
        };
    }

    static ExpectationHandler queryListingWithParamHandler( final Map<String, Object> queryParams,
                                                            final String... resources )
    {
        return ( req, res ) -> {
            if ( !isParamEquals( queryParams, req ) )
            {
                res.setStatus( SC_NOT_FOUND );
                return;
            }
            res.setStatus( SC_OK );
            final String json = Utils.generateStoreListingContent( resources );
            res.getWriter().write( json );
        };
    }

    private static boolean isParamEquals( final Map<String, Object> queryParams, HttpServletRequest req )
    {
        for ( Map.Entry<String, Object> e : queryParams.entrySet() )
        {
            String paramValue = req.getParameter( e.getKey() );

            if ( ( e.getValue() instanceof String ) && !e.getValue().equals( paramValue ) )
            {
                return false;
            }
            if ( e.getValue() instanceof Set )
            {
                Set<String> paramValueSet =
                        Arrays.stream( paramValue.split( "," ) ).map( String::trim ).collect( Collectors.toSet() );
                if ( !e.getValue().equals( paramValueSet ) )
                {
                    return false;
                }

            }
        }
        return true;
    }

    static String generateStoreListingContent( final String... resources )
            throws IOException
    {
        final StringBuilder contentBdr = new StringBuilder();
        final Object[] resContents = new String[resources.length];
        int i = 0;
        for ( final String res : resources )
        {
            contentBdr.append( "%s," );
            resContents[i] = readResource( res );
            i++;
        }
        contentBdr.deleteCharAt( contentBdr.length() - 1 );
        return String.format( "{\"items\":[" + contentBdr + "]}", resContents );
    }
}
