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
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.galley.GroupLocation;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.test.fixture.core.TestCacheProvider;
import org.commonjava.indy.test.fixture.core.TestFileEventManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static org.commonjava.indy.content.ContentManager.ENTRY_POINT_BASE_URI;
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;
import static org.junit.Assert.assertEquals;

/**
 * Test the npm masking decorator which converts dist/tarball url to indy url.
 */
public class NPMPackageMaskingTransferDecoratorTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testDecorator() throws Exception
    {
        String path = "package.json";
        KeyedLocation location = new GroupLocation( PKG_TYPE_NPM, "test" );
        File file = new File( temp.newFolder( location.getName() ), path );

        IOUtils.copy( getResourceAsStream( "metadata/package-1.json" ), new FileOutputStream( file ) );

        ConcreteResource resource = new ConcreteResource( location, path );
        TestCacheProvider provider = new TestCacheProvider( temp.getRoot(), new TestFileEventManager(),
                                                            new TransferDecoratorManager( new NPMPackageMaskingTransferDecorator() ) );
        Transfer transfer = provider.getTransfer( resource );

        InputStream stream = transfer.openInputStream( false, new EventMetadata().set( ENTRY_POINT_BASE_URI,
                                                                                       "http://localhost/api/content/npm" ) );
        String ret = IOUtils.toString( stream );

        String expected = IOUtils.toString( getResourceAsStream( "metadata/package-1-decorated.json" ) );
        assertEquals( expected, ret );
    }

    @Test
    public void testDecorator2() throws Exception
    {
        String path = "package.json";
        KeyedLocation location = new GroupLocation( PKG_TYPE_NPM, "test" );
        File file = new File( temp.newFolder( location.getName() ), path );

        IOUtils.copy( getResourceAsStream( "metadata/package-tar-fs.json" ), new FileOutputStream( file ) );

        ConcreteResource resource = new ConcreteResource( location, path );
        TestCacheProvider provider = new TestCacheProvider( temp.getRoot(), new TestFileEventManager(),
                                                            new TransferDecoratorManager( new NPMPackageMaskingTransferDecorator() ) );
        Transfer transfer = provider.getTransfer( resource );

        InputStream stream = transfer.openInputStream( false, new EventMetadata().set( ENTRY_POINT_BASE_URI,
                                                                                       "http://localhost/api/content/npm" ) );
        String ret = IOUtils.toString( stream );

        String expected = IOUtils.toString( getResourceAsStream( "metadata/package-tar-fs-decorated.json" ) );
        assertEquals( expected, ret );
    }

    private InputStream getResourceAsStream( String path )
    {
        return getClass().getClassLoader().getResourceAsStream( path );
    }

}
