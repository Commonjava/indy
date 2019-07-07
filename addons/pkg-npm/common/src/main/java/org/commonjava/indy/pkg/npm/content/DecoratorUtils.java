package org.commonjava.indy.pkg.npm.content;

import org.commonjava.maven.galley.util.UrlUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DecoratorUtils
{
    /**
     * Replace tarball urls with context urls, e.g., "https://registry.npmjs.org/jquery/-/jquery-1.5.1.tgz" to
     * "http://${indy}/api/content/npm/remote/test/jquery/-/jquery-1.5.1.tgz".
     */
    public static String updatePackageJson( String raw, String contextURL ) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int index;
        while ( ( index = raw.indexOf( "\"tarball\"" ) ) >= 0 )
        {
            int colon = raw.indexOf( ":", index );
            String s = raw.substring( 0, colon + 1 );
            sb.append( s );

            raw = raw.substring( colon + 1 );
            int quote = raw.indexOf( "\"" );
            s = raw.substring( 0, quote ); // blanks between : and "
            sb.append( s );

            int nextQuote = raw.indexOf( "\"", quote + 1 );
            String url = raw.substring( quote + 1, nextQuote );
            String path = getPath( url );

            url = UrlUtils.buildUrl( contextURL, path );
            sb.append( "\"" + url + "\"" );
            raw = raw.substring( nextQuote + 1 );
        }
        sb.append( raw );
        return sb.toString();
    }

    private static String getPath( String url ) throws IOException
    {
        URL url1;
        try
        {
            url1 = new URL( url );
        }
        catch ( MalformedURLException e )
        {
            throw new IOException( "Failed to parse URL " + url, e ); // should not happen
        }
        return url1.getPath();
    }

}
