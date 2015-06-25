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
package org.commonjava.aprox.httprox.util;

import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.commonjava.aprox.util.ApplicationHeader;

public final class UserPass
{
    private final String user;

    private final String password;

    public static UserPass parse( final ApplicationHeader header, final List<String> headerLines, String userpass )
    {
        if ( userpass == null )
        {
            for ( final String line : headerLines )
            {
                final String upperLine = line.toUpperCase();
                if ( upperLine.startsWith( header.upperKey() ) && upperLine.contains( "BASIC" ) )
                {
                    final String[] authParts = line.split( " " );
                    if ( authParts.length > 2 )
                    {
                        userpass = new String( Base64.decodeBase64( authParts[2] ) );
                    }
                }
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
}