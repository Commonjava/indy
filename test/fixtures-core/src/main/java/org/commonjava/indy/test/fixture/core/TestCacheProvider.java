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
package org.commonjava.indy.test.fixture.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.maven.galley.cache.SimpleLockingSupport;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestCacheProvider
    implements CacheProvider
{

    private final File dir;

    private final FileEventManager events;

    private final TransferDecoratorManager decorator;

    private final SimpleLockingSupport lockingSupport = new SimpleLockingSupport();

    public TestCacheProvider( final File dir, final FileEventManager events, final TransferDecoratorManager decorator )
    {
        this.dir = dir;
        this.events = events;
        this.decorator = decorator;
    }

    public Transfer writeClasspathResourceToCache( final ConcreteResource resource, final String cpResource )
        throws IOException
    {
        final InputStream in = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( cpResource );
        if ( in == null )
        {
            throw new IOException( "Classpath resource not found: " + cpResource );
        }

        final Transfer tx = getTransfer( resource );
        OutputStream out = null;
        try
        {
            out = tx.openOutputStream( TransferOperation.UPLOAD, false );
            IOUtils.copy( in, out );
        }
        finally
        {
            IOUtils.closeQuietly( in );
            IOUtils.closeQuietly( out );
        }

        return tx;
    }

    public Transfer writeToCache( final ConcreteResource resource, final String content )
        throws IOException
    {
        if ( content == null )
        {
            throw new IOException( "Content is empty!" );
        }

        final Transfer tx = getTransfer( resource );
        OutputStream out = null;
        try
        {
            out = tx.openOutputStream( TransferOperation.UPLOAD, false );
            out.write( content.getBytes() );
        }
        finally
        {
            IOUtils.closeQuietly( out );
        }

        return tx;
    }

    @Override
    public boolean isDirectory( final ConcreteResource resource )
    {
        return !getDetachedFile( resource ).isFile();
    }

    @Override
    public boolean isFile( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).isFile();
    }

    @Override
    public InputStream openInputStream( final ConcreteResource resource )
        throws IOException
    {
        return new FileInputStream( getDetachedFile( resource ) );
    }

    @Override
    public OutputStream openOutputStream( final ConcreteResource resource )
        throws IOException
    {
        final File f = getDetachedFile( resource );
        final File d = f.getParentFile();
        if ( d != null )
        {
            d.mkdirs();
        }

        return new FileOutputStream( f );
    }

    @Override
    public boolean exists( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).exists();
    }

    @Override
    public void copy( final ConcreteResource from, final ConcreteResource to )
        throws IOException
    {
        final File ff = getDetachedFile( from );
        final File tf = getDetachedFile( to );
        if ( ff.isDirectory() )
        {
            FileUtils.copyDirectory( ff, tf );
        }
        else
        {
            FileUtils.copyFile( ff, tf );
        }
    }

    @Override
    public String getFilePath( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).getPath();
    }

    @Override
    public boolean delete( final ConcreteResource resource )
        throws IOException
    {
        FileUtils.forceDelete( getDetachedFile( resource ) );
        return true;
    }

    @Override
    public String[] list( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).list();
    }

    public File getDetachedFile( final ConcreteResource resource )
    {
        return new File( new File( dir, resource.getLocationName() ), resource.getPath() );
    }

    @Override
    public void mkdirs( final ConcreteResource resource )
        throws IOException
    {
        getDetachedFile( resource ).mkdirs();
    }

    @Override
    public void createFile( final ConcreteResource resource )
        throws IOException
    {
        getDetachedFile( resource ).createNewFile();
    }

    @Override
    public void createAlias( final ConcreteResource from, final ConcreteResource to )
        throws IOException
    {
        final File fromFile = getDetachedFile( from );
        final File toFile = getDetachedFile( to );
        FileUtils.copyFile( fromFile, toFile );
        //        Files.createLink( Paths.get( fromFile.toURI() ), Paths.get( toFile.toURI() ) );
    }

    @Override
    public void clearTransferCache()
    {
    }

    @Override
    public Transfer getTransfer( final ConcreteResource resource )
    {
        return new Transfer( resource, this, events, decorator );
    }

    @Override
    public long length( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).length();
    }

    @Override
    public long lastModified( final ConcreteResource resource )
    {
        return getDetachedFile( resource ).lastModified();
    }

    @Override
    public boolean isReadLocked( final ConcreteResource resource )
    {
        return lockingSupport.isLocked( resource );
    }

    @Override
    public boolean isWriteLocked( final ConcreteResource resource )
    {
        return lockingSupport.isLocked( resource );
    }

    @Override
    public void unlockRead( final ConcreteResource resource )
    {
        lockingSupport.unlock( resource );
    }

    @Override
    public void unlockWrite( final ConcreteResource resource )
    {
        lockingSupport.unlock( resource );
    }

    @Override
    public void lockRead( final ConcreteResource resource )
    {
        lockingSupport.lock( resource );
    }

    @Override
    public void lockWrite( final ConcreteResource resource )
    {
        lockingSupport.lock( resource );
    }

    @Override
    public void waitForWriteUnlock( final ConcreteResource resource )
    {
        lockingSupport.waitForUnlock( resource );
    }

    @Override
    public void waitForReadUnlock( final ConcreteResource resource )
    {
        lockingSupport.waitForUnlock( resource );
    }

    @Override
    public AdminView asAdminView()
    {
        throw new UnsupportedOperationException( "No support for AdminView" );
    }

    @Override
    public void cleanupCurrentThread()
    {
        lockingSupport.cleanupCurrentThread();
    }

    @Override
    public void startReporting()
    {
        lockingSupport.startReporting();
    }

    @Override
    public void stopReporting()
    {
        lockingSupport.stopReporting();
    }
}
