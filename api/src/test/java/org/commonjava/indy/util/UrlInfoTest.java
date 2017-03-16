package org.commonjava.indy.util;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import java.util.regex.Pattern;

public class UrlInfoTest
{
    private static final Pattern IP_REGEX = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$" );

    @Test
    public void testUrlInfo()
            throws Exception
    {
        UrlInfo urlInfo = new UrlInfo( "http://repo.maven.apache.org/" );
        assertThat( urlInfo.getHost(), equalTo( "repo.maven.apache.org" ) );
        assertThat( urlInfo.getPort(), equalTo( 80 ) );
        assertThat( urlInfo.getFileWithNoLastSlash(), equalTo( "" ) );
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org" ) );
        assertThat( IP_REGEX.matcher( urlInfo.getIpForUrl() ).matches(), equalTo( true ) );

        urlInfo = new UrlInfo( "https://repo.maven.apache.org/" );
        assertThat( urlInfo.getHost(), equalTo( "repo.maven.apache.org" ) );
        assertThat( urlInfo.getPort(), equalTo( 443 ) );
        assertThat( urlInfo.getFileWithNoLastSlash(), equalTo( "" ) );
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org" ) );
        assertThat( IP_REGEX.matcher( urlInfo.getIpForUrl() ).matches(), equalTo( true ) );

        urlInfo = new UrlInfo( "https://repo.maven.apache.org/org/commonjava/indy/" );
        assertThat( urlInfo.getHost(), equalTo( "repo.maven.apache.org" ) );
        assertThat( urlInfo.getPort(), equalTo( 443 ) );
        assertThat( urlInfo.getFileWithNoLastSlash(), equalTo( "/org/commonjava/indy" ) );
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org/org/commonjava/indy" ) );
        assertThat( IP_REGEX.matcher( urlInfo.getIpForUrl() ).matches(), equalTo( true ) );

        urlInfo = new UrlInfo( "http://repo.maven.apache.org:8080/org/commonjava/indy/" );
        assertThat( urlInfo.getHost(), equalTo( "repo.maven.apache.org" ) );
        assertThat( urlInfo.getPort(), equalTo( 8080 ) );
        assertThat( urlInfo.getFileWithNoLastSlash(), equalTo( "/org/commonjava/indy" ) );
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org:8080/org/commonjava/indy" ) );
        assertThat( IP_REGEX.matcher( urlInfo.getIpForUrl() ).matches(), equalTo( true ) );

        urlInfo = new UrlInfo( "http://repo.maven.apache.org:8080/org/commonjava/indy/a.html?klj=skljdflkf" );
        assertThat( urlInfo.getHost(), equalTo( "repo.maven.apache.org" ) );
        assertThat( urlInfo.getPort(), equalTo( 8080 ) );
        assertThat( urlInfo.getFileWithNoLastSlash(), equalTo( "/org/commonjava/indy/a.html?klj=skljdflkf" ) );
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org:8080/org/commonjava/indy/a.html?klj=skljdflkf" ) );
        assertThat( IP_REGEX.matcher( urlInfo.getIpForUrl() ).matches(), equalTo( true ) );
    }
}
