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
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org:80" ) );
        assertThat( IP_REGEX.matcher( urlInfo.getIpForUrl() ).matches(), equalTo( true ) );

        urlInfo = new UrlInfo( "https://repo.maven.apache.org/" );
        assertThat( urlInfo.getHost(), equalTo( "repo.maven.apache.org" ) );
        assertThat( urlInfo.getPort(), equalTo( 443 ) );
        assertThat( urlInfo.getFileWithNoLastSlash(), equalTo( "" ) );
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org:443" ) );
        assertThat( IP_REGEX.matcher( urlInfo.getIpForUrl() ).matches(), equalTo( true ) );

        urlInfo = new UrlInfo( "https://repo.maven.apache.org/org/commonjava/indy/" );
        assertThat( urlInfo.getHost(), equalTo( "repo.maven.apache.org" ) );
        assertThat( urlInfo.getPort(), equalTo( 443 ) );
        assertThat( urlInfo.getFileWithNoLastSlash(), equalTo( "/org/commonjava/indy" ) );
        assertThat( urlInfo.getUrlWithNoSchemeAndLastSlash(), equalTo( "repo.maven.apache.org:443/org/commonjava/indy" ) );
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
