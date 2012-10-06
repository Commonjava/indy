/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.filer.def;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.filer.PathUtils;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.io.StorageProvider;
import org.commonjava.aprox.model.StoreKey;

@Singleton
public class DefaultStorageProvider
    implements StorageProvider
{

    @Inject
    private DefaultStorageProviderConfiguration filerConfig;

    public DefaultStorageProvider()
    {
    }

    public DefaultStorageProvider( final DefaultStorageProviderConfiguration filterConfig )
    {
        this.filerConfig = filterConfig;
    }

    @Override
    public boolean exists( final StoreKey key, final String path )
    {
        return getDetachedFile( key, path ).exists();
    }

    @Override
    public boolean isDirectory( final StoreKey key, final String path )
    {
        return getDetachedFile( key, path ).isDirectory();
    }

    @Override
    public InputStream openInputStream( final StoreKey key, final String path )
        throws IOException
    {
        return new FileInputStream( getDetachedFile( key, path ) );
    }

    @Override
    public OutputStream openOutputStream( final StoreKey key, final String path )
        throws IOException
    {
        final File file = getDetachedFile( key, path );

        final File dir = file.getParentFile();
        if ( !dir.isDirectory() && !dir.mkdirs() )
        {
            throw new IOException( "Cannot create directory: " + dir );
        }

        return new FileOutputStream( file );
    }

    @Override
    public void copy( final StoreKey fromKey, final String fromPath, final StoreKey toKey, final String toPath )
        throws IOException
    {
        final File from = getDetachedFile( fromKey, fromPath );
        final File to = getDetachedFile( toKey, toPath );
        FileUtils.copyFile( from, to );
    }

    @Override
    public String getFilePath( final StoreKey key, final String path )
    {
        return getDetachedFile( key, path ).getAbsolutePath();
    }

    @Override
    public void delete( final StoreKey key, final String path )
        throws IOException
    {
        getDetachedFile( key, path ).delete();
    }

    @Override
    public String[] list( final StoreKey key, final String path )
    {
        return getDetachedFile( key, path ).list();
    }

    @Override
    public File getDetachedFile( final StoreKey key, final String path )
    {
        return new File( filerConfig.getStorageRootDirectory(), storePath( key, path ) );
    }

    public String storePath( final StoreKey key, final String path )
    {
        final String name = key.getType()
                               .name() + "-" + key.getName();

        return PathUtils.join( name, path );
    }
}
