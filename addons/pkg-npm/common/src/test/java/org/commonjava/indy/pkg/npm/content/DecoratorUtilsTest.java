package org.commonjava.indy.pkg.npm.content;

import org.junit.Test;

import static org.commonjava.indy.pkg.npm.content.DecoratorUtils.updatePackageJson;
import static org.junit.Assert.assertEquals;

public class DecoratorUtilsTest
{

    String raw = "\"versions\": {\n"
                    + "\"1.5.1\": {\n"
                    + "  \"dist\": \n"
                    + "  {\n"
                    + "    \"shasum\": \"2ae2d661e906c1a01e044a71bb5b2743942183e5\",\n"
                    + "    \"tarball\": \"http://registry.npmjs.org/jquery/-/jquery-1.5.1.tgz\"\n"
                    + "  },\n"
                    + "\"1.5.2\": {\n"
                    + "  \"dist\": \n"
                    + "  {\n"
                    + "    \"shasum\": \"et3d7g61e906c1a01e044a71bb5b27439421834t\",\n"
                    + "    \"tarball\": \"http://registry.npmjs.redhat.com/jquery/-/jquery-1.5.2.tgz\"\n"
                    + "  }\n"
                    + "}";

    String expected = "\"versions\": {\n"
                    + "\"1.5.1\": {\n"
                    + "  \"dist\": \n"
                    + "  {\n"
                    + "    \"shasum\": \"2ae2d661e906c1a01e044a71bb5b2743942183e5\",\n"
                    + "    \"tarball\": \"http://indy.psi.redhat.com/api/content/group/a/jquery/-/jquery-1.5.1.tgz\"\n"
                    + "  },\n"
                    + "\"1.5.2\": {\n"
                    + "  \"dist\": \n"
                    + "  {\n"
                    + "    \"shasum\": \"et3d7g61e906c1a01e044a71bb5b27439421834t\",\n"
                    + "    \"tarball\": \"http://indy.psi.redhat.com/api/content/group/a/jquery/-/jquery-1.5.2.tgz\"\n"
                    + "  }\n"
                    + "}";

    @Test
    public void testUpdateString() throws Exception
    {
        String contextURL = "http://indy.psi.redhat.com/api/content/group/a";
        String s = updatePackageJson( raw, contextURL );
        //System.out.println( ">>>\n" + s );
        assertEquals( expected, s );
    }

}
