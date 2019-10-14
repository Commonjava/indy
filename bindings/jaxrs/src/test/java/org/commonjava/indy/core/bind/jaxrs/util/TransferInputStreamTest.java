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
package org.commonjava.indy.core.bind.jaxrs.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TransferInputStreamTest
{
    final String testString = "This is a test string";

    @Test
    public void testSingleRead()
            throws IOException
    {
        ByteArrayInputStream bStream = new ByteArrayInputStream( testString.getBytes() );
        TransferCountingInputStream tStream = new TransferCountingInputStream( bStream, null, null );
        tStream.read();
        tStream.close();

        assertThat( tStream.getSize(), equalTo( 1L ) );
    }

    @Test
    public void testFixedRead1()
            throws IOException
    {
        ByteArrayInputStream bStream = new ByteArrayInputStream( testString.getBytes() );
        TransferCountingInputStream tStream = new TransferCountingInputStream( bStream, null, null );
        byte[] b = new byte[5];
        tStream.read( b );
        tStream.close();

        assertThat( tStream.getSize(), equalTo( 5L ) );
        assertThat( new String( b ), equalTo( "This " ) );
    }

    @Test
    public void testFixedRead2()
            throws IOException
    {
        ByteArrayInputStream bStream = new ByteArrayInputStream( testString.getBytes() );
        TransferCountingInputStream tStream = new TransferCountingInputStream( bStream, null, null );
        byte[] b = new byte[8];
        b[0] = b[1] = b[2] = ' ';
        tStream.read( b, 3, 5 );
        tStream.close();

        assertThat( tStream.getSize(), equalTo( 5L ) );
        assertThat( new String( b ), equalTo( "   This " ) );
    }
    @Test
    public void testFullRead()
            throws IOException
    {
        ByteArrayInputStream bStream = new ByteArrayInputStream( testString.getBytes() );
        TransferCountingInputStream tStream = new TransferCountingInputStream( bStream, null, null );
        byte[] b = new byte[testString.length()];
        tStream.read( b );
        tStream.close();

        assertThat( tStream.getSize(), equalTo( Integer.toUnsignedLong( testString.length() ) ) );
        assertThat( new String(b), equalTo( testString ));
    }
}
