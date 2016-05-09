package org.commonjava.indy.content.index;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by jdcasey on 5/7/16.
 */
public class DigestTest
{
    @Test
    public void sha1VsMd5VsUtf8VsStringLengths()
    {
        String p1 =
                "/org/commonjava/indy/launch/indy-launcher-savant/0.99.3/indy-launcher-savant-0.99.3-launcher.tar.gz";
        String p2 = "/org/commonjava/commonjava/10/commonjava-10.pom";

        String p3 = "remote:central";

        Stream.of( p1, p2, p3 ).forEach( ( path)->
        {
            try
            {
                byte[] raw = path.getBytes( "UTF-8" );
                byte[] sha1 = DigestUtils.sha( path );
                byte[] md5 = DigestUtils.md5( path );

                System.out.printf( "%s\n    Raw length: %s, byte length: %s, SHA1 length: %s, MD5 length: %s\n\n", path, path.length(), raw.length,
                                   sha1.length, md5.length );
            }
            catch ( UnsupportedEncodingException e )
            {
                e.printStackTrace();
                Assert.fail( "Failed to test digest lengths for: '" + path + "'" );
            }
        });
    }

}
