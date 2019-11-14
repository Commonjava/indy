/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.pathmap.migrate;

import org.apache.commons.io.IOUtils;
import org.commonjava.storage.pathmapped.util.ChecksumCalculator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ChecksumCalculatorTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String dupContent = "abc";

    private String otherContent = "def";

    private File f1, f2, f3;

    @Before
    public void prepare() throws Exception
    {
        f1 = temporaryFolder.newFile( "f1" );
        try (FileOutputStream out = new FileOutputStream( f1 ))
        {
            IOUtils.write( dupContent, out );
        }
        f2 = temporaryFolder.newFile( "f2" );
        try (FileOutputStream out = new FileOutputStream( f2 ))
        {
            IOUtils.write( dupContent, out );
        }
        f3 = temporaryFolder.newFile( "f3" );
        try (FileOutputStream out = new FileOutputStream( f3 ))
        {
            IOUtils.write( otherContent, out );
        }
    }

    @Ignore
    @Test
    public void run() throws Exception
    {
        String s1 = calculateChecksum( f1 );
        System.out.println( ">>> s1=" + s1 );

        String s2 = calculateChecksum( f2 );
        System.out.println( ">>> s2=" + s2 );

        String s3 = calculateChecksum( f3 );
        System.out.println( ">>> s3=" + s3 );

        assertEquals( s1, s2 );
        assertNotEquals( s1, s3 );
    }

    private String calculateChecksum( File file ) throws Exception
    {
        ChecksumCalculator checksumCalculator = new ChecksumCalculator( "MD5" );

        if ( !file.exists() || !file.isFile() )
        {
            throw new IOException(
                            String.format( "Digest error: file not exists or not a regular file for file %s", file ) );
        }
        try (FileInputStream is = new FileInputStream( file ))
        {
            checksumCalculator.update( IOUtils.toByteArray( is ) );
        }
        return checksumCalculator.getDigestHex();
    }
}
