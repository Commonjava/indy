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
package org.commonjava.indy.subsys.git;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by gli on 12/16/16.
 */
public class AbstractGitManagerTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    protected File unpackRepo( final String resource )
            throws Exception
    {
        final URL url = Thread.currentThread()
                              .getContextClassLoader()
                              .getResource( resource );

        final InputStream stream = url.openStream();
        final ZipInputStream zstream = new ZipInputStream( stream );

        final File dir = temp.newFolder();

        ZipEntry entry = null;
        while ( ( entry = zstream.getNextEntry() ) != null )
        {
            final File f = new File( dir, entry.getName() );
            if ( entry.isDirectory() )
            {
                f.mkdirs();
            }
            else
            {
                f.getParentFile()
                 .mkdirs();
                final OutputStream out = new FileOutputStream( f );

                copy( zstream, out );

                closeQuietly( out );
            }

            zstream.closeEntry();
        }

        closeQuietly( zstream );

        final File root = new File( dir, "test-indy-data/.git" );
        assertThat( root.exists(), equalTo( true ) );
        assertThat( root.isDirectory(), equalTo( true ) );

        return root;
    }
}
