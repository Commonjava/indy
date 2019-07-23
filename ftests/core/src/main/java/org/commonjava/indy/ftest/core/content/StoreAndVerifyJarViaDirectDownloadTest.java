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

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class StoreAndVerifyJarViaDirectDownloadTest
        extends AbstractContentManagementTest
{

    @Test
    public void storeFileThenDownloadAndVerifyContentViaDirectDownload()
            throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        String entryName = "org/something/foo.class";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JarOutputStream jarOut = new JarOutputStream( out );
        jarOut.putNextEntry( new JarEntry( entryName ) );
        jarOut.write( content.getBytes() );
        jarOut.close();

        // Used to visually inspect the jars moving up...
        //        String userDir = System.getProperty( "user.home" );
        //        File dir = new File( userDir, "temp" );
        //        dir.mkdirs();
        //
        //        FileUtils.writeByteArrayToFile( new File( dir, name.getMethodName() + "-in.jar" ), out.toByteArray() );

        final InputStream stream = new ByteArrayInputStream( out.toByteArray() );

        final String path = "/path/to/" + getClass().getSimpleName() + "-" + name.getMethodName() + ".jar";

        assertThat( client.content().exists( hosted, STORE, path ), equalTo( false ) );

        client.content().store( hosted, STORE, path, stream );

        assertThat( client.content().exists( hosted, STORE, path ), equalTo( true ) );

        final URL url = new URL( client.content().contentUrl( hosted, STORE, path ) );

        final InputStream is = url.openStream();

        byte[] result = IOUtils.toByteArray( is );
        is.close();

        assertThat( result, equalTo( out.toByteArray() ) );

        // ...and down
        //        FileUtils.writeByteArrayToFile( new File( dir, name.getMethodName() + "-out.jar" ), result );

        JarInputStream jarIn = new JarInputStream( new ByteArrayInputStream( result ) );
        JarEntry jarEntry = jarIn.getNextJarEntry();

        assertThat( jarEntry.getName(), equalTo( entryName ) );
        String contentResult = IOUtils.toString( jarIn );

        assertThat( contentResult, equalTo( content ) );
    }
}
