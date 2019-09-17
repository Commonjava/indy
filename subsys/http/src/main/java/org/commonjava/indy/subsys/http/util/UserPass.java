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
package org.commonjava.indy.subsys.http.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.commonjava.indy.util.ApplicationHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserPass
{
    //    private static final Logger logger = LoggerFactory.getLogger( UserPass.class );

    private final String user;

    private final String password;

    public static UserPass parse( final ApplicationHeader header, final HttpRequest httpRequest, String userpass )
    {
        Logger logger = LoggerFactory.getLogger( UserPass.class );
        if ( userpass == null )
        {
            Header[] headers = httpRequest.getHeaders( header.key() );
            if ( headers != null && headers.length > 0 )
            {
                for ( Header h : headers )
                {
                    String value = h.getValue();
                    if ( value.toUpperCase().startsWith( "BASIC " ) )
                    {
                        final String[] authParts = value.split( " " );
                        userpass = new String( Base64.decodeBase64( authParts[1] ) );
                        logger.info( "userpass is: '{}'", userpass );
                        break;
                    }
                }
            }
        }

        if ( userpass != null )
        {
            final String[] upStr = userpass.split( ":" );
            logger.info( "Split userpass into:\n  {}", StringUtils.join( upStr, "\n  " ) );

            if ( upStr.length < 1 )
            {
                return null;
            }

            return new UserPass( upStr[0], upStr.length > 1 ? upStr[1] : null );
        }

        return null;
    }

    public static UserPass parse( final String headerValue )
    {
        if ( StringUtils.isEmpty( headerValue ) )
        {
            return null;
        }

        String userpass = null;
        final String upperHeader = headerValue.toUpperCase();
        //        logger.debug( "upper-case header value: '{}'", upperHeader );
        if ( upperHeader.startsWith( "BASIC" ) )
        {
            final String[] authParts = headerValue.split( " " );
            //            logger.debug( "split into: '{}'", Arrays.toString( authParts ) );
            if ( authParts.length > 1 )
            {
                userpass = new String( Base64.decodeBase64( authParts[1] ) );
                //                logger.debug( "Decoded BASIC userpass: {}", userpass );
            }
        }

        if ( userpass != null )
        {
            final String[] upStr = userpass.split( ":" );

            if ( upStr.length < 1 )
            {
                return null;
            }

            return new UserPass( upStr[0], upStr.length > 1 ? upStr[1] : null );
        }

        return null;
    }

    public UserPass( final String user, final String password )
    {
        this.user = user;
        this.password = password;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    @Override
    public String toString()
    {
        return "User-Pass [" + user + ":" + ( password == null ? "none" : "********" ) + "]";
    }
}