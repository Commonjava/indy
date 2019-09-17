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
package org.commonjava.indy.ftest.core.content;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A hosted with org/foo/bar/1/bar-1.pom</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Access org/foo/bar/1/bar-1.pom.md5 in the hosted</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The .md5 is generated and returned</li>
 * </ul>
 */
public class HostedGenerateMissingChecksumTest
                extends AbstractContentManagementTest
{
    private HostedRepository hosted;

    private final String HOSTED = "hosted";

    private final String POM_PATH = "org/foo/bar/1/bar-1.pom";

    private final String POM_MD5_PATH = "org/foo/bar/1/bar-1.pom.md5";

    private final String POM_SHA1_PATH = "org/foo/bar/1/bar-1.pom.sha1";

    private final String POM_CONTENT = "This is the pom";

    private final byte[] POM_CONTENT_BYTES = POM_CONTENT.getBytes();

    @Before
    public void setupTest() throws Exception
    {
        String change = "setup";
        hosted = client.stores().create( new HostedRepository( "maven", HOSTED ), change, HostedRepository.class );
        client.content().store( hosted.getKey(), POM_PATH, new ByteArrayInputStream( POM_CONTENT.getBytes() ) );
    }

    @Test
    public void run() throws Exception
    {
        String expectedMD5 = Hex.encodeHexString( MessageDigest.getInstance( "MD5" ).digest( POM_CONTENT_BYTES ) );
        String expectedSHA1 = Hex.encodeHexString( MessageDigest.getInstance( "SHA-1" ).digest( POM_CONTENT_BYTES ) );

        try (InputStream inputStream = client.content().get( hosted.getKey(), POM_PATH ))
        {
            assertThat( inputStream, notNullValue() );
        }

        validateIt(POM_MD5_PATH, expectedMD5);
        validateIt(POM_SHA1_PATH, expectedSHA1);
    }

    private void validateIt( String checksumPath, String expected ) throws Exception
    {
        try (InputStream inputStream = client.content().get( hosted.getKey(), checksumPath ))
        {
            assertThat( inputStream, notNullValue() );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy( inputStream, out );
            String checksum = out.toString();
            //System.out.println( ">>>" + checksum );
            assertThat( checksum, equalTo( expected ) );
        }
    }
}
