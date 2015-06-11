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
            return new UserPass( upStr[0], upStr[1] );
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