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
package org.commonjava.indy.promote.validate.util;

import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdcasey on 9/22/15.
 */
public class ReadOnlyTransfer
    extends Transfer
{
    private Transfer delegate;

    public static Transfer readOnlyWrapper( Transfer transfer )
    {
        return transfer == null ? null : new ReadOnlyTransfer( transfer );
    }

    public static List<Transfer> readOnlyWrappers( List<Transfer> transfers )
    {
        if( transfers == null )
        {
            return null;
        }
        if ( transfers.isEmpty() )
        {
            return transfers;
        }

        List<Transfer> result = new ArrayList<>( transfers.size() );
        for ( Transfer transfer : transfers )
        {
            result.add( readOnlyWrapper( transfer ) );
        }

        return result;
    }

    public ReadOnlyTransfer( Transfer transfer )
    {
        super( transfer.getResource(), null, null, null );
        delegate = transfer;
    }

    @Override
    public boolean isDirectory()
    {
        return delegate.isDirectory();
    }

    @Override
    public boolean isFile()
    {
        return delegate.isFile();
    }

    @Override
    public Location getLocation()
    {
        return delegate.getLocation();
    }

    @Override
    public String getPath()
    {
        return delegate.getPath();
    }

    @Override
    public ConcreteResource getResource()
    {
        return delegate.getResource();
    }

    @Override
    public String toString()
    {
        return "[READ-ONLY] " + delegate.toString();
    }

    @Override
    public Transfer getParent()
    {
        return readOnlyWrapper( delegate.getParent() );
    }

    @Override
    public Transfer getChild( String file )
    {
        return readOnlyWrapper( delegate.getChild( file ) );
    }

    @Override
    public void touch()
    {
    }

    @Override
    public void touch( EventMetadata eventMetadata )
    {
    }

    @Override
    public InputStream openInputStream()
            throws IOException
    {
        return delegate.openInputStream();
    }

    @Override
    public InputStream openInputStream( boolean fireEvents )
            throws IOException
    {
        return delegate.openInputStream( fireEvents );
    }

    @Override
    public InputStream openInputStream( boolean fireEvents, EventMetadata eventMetadata )
            throws IOException
    {
        return delegate.openInputStream( fireEvents, eventMetadata );
    }

    @Override
    public OutputStream openOutputStream( TransferOperation accessType )
            throws IOException
    {
        deny();
        return null;
    }

    @Override
    public OutputStream openOutputStream( TransferOperation accessType, boolean fireEvents )
            throws IOException
    {
        deny();
        return null;
    }

    @Override
    public OutputStream openOutputStream( TransferOperation accessType, boolean fireEvents,
                                          EventMetadata eventMetadata )
            throws IOException
    {
        deny();
        return null;
    }

    @Override
    public OutputStream openOutputStream( TransferOperation accessType, boolean fireEvents,
                                          EventMetadata eventMetadata, boolean deleteFilesOnPath )
            throws IOException
    {
        deny();
        return null;
    }

    @Override
    public boolean exists()
    {
        return delegate.exists();
    }

    @Override
    public void copyFrom( Transfer f )
            throws IOException
    {
        deny();
    }

    private void deny()
            throws IOException
    {
        throw new IOException( toString() + ": Cannot write to read-only transfer!" );
    }

    @Override
    public String getFullPath()
    {
        return delegate.getFullPath();
    }

    @Override
    public boolean delete()
            throws IOException
    {
        deny();
        return false;
    }

    @Override
    public boolean delete( boolean fireEvents )
            throws IOException
    {
        deny();
        return false;
    }

    @Override
    public boolean delete( boolean fireEvents, EventMetadata eventMetadata )
            throws IOException
    {
        deny();
        return false;
    }

    @Override
    public String[] list()
            throws IOException
    {
        return delegate.list();
    }

//    @Override
//    // FIXME: This is a leak of the read-only mechanism, but may be necessary.
//    public File getDetachedFile()
//    {
//        return delegate.getDetachedFile();
//    }

    @Override
    public void mkdirs()
            throws IOException
    {
        deny();
    }

    @Override
    public void createFile()
            throws IOException
    {
        deny();
    }

    @Override
    public long length()
    {
        return delegate.length();
    }

    @Override
    public long lastModified()
    {
        return delegate.lastModified();
    }

    @Override
    public Transfer getSibling( String named )
    {
        return readOnlyWrapper( delegate.getSibling( named ) );
    }

    @Override
    public Transfer getSiblingMeta( String extension )
    {
        return readOnlyWrapper( delegate.getSiblingMeta( extension ) );
    }

    @Override
    public void lockWrite()
    {
    }

    @Override
    public void unlock()
    {
    }

    @Override
    public boolean isWriteLocked()
    {
        return delegate.isWriteLocked();
    }
}
