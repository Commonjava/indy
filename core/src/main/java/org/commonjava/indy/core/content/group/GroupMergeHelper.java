/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.core.content.group;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.indy.util.LocationUtils.getKey;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import javax.inject.Inject;

import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupMergeHelper
{

    public static final String MERGEINFO_SUFFIX = ".info";

    public static final String SHA_SUFFIX = ".sha";

    public static final String MD5_SUFFIX = ".md5";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DownloadManager downloadManager;

    protected GroupMergeHelper()
    {
    }

    public GroupMergeHelper( final DownloadManager downloadManager )
    {
        this.downloadManager = downloadManager;
    }

    public final void deleteChecksumsAndMergeInfo( final Group group, final String path )
        throws IOException
    {
        final Transfer targetSha = downloadManager.getStorageReference( group, path + SHA_SUFFIX );
        final Transfer targetMd5 = downloadManager.getStorageReference( group, path + MD5_SUFFIX );
        final Transfer targetInfo = downloadManager.getStorageReference( group, path + MERGEINFO_SUFFIX );

        if ( targetSha != null )
        {
            logger.debug( "Deleting: {}", targetSha );
            targetSha.delete();
        }

        if ( targetMd5 != null )
        {
            logger.debug( "Deleting: {}", targetMd5 );
            targetMd5.delete();
        }

        if ( targetInfo != null )
        {
            logger.debug( "Deleting: {}", targetInfo );
            targetInfo.delete();
        }
    }

    public final void writeMergeInfo( final byte[] data, final List<Transfer> sources, final Group group,
                                      final String path )
    {
        final Transfer targetInfo = downloadManager.getStorageReference( group, path + MERGEINFO_SUFFIX );

        Writer fw = null;
        try
        {
            fw = new OutputStreamWriter( targetInfo.openOutputStream( TransferOperation.GENERATE ) );
            for ( final Transfer source : sources )
            {
                final StoreKey key = getKey( source );
                fw.write( key.toString() );
                fw.write( "\n" );
            }
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to write merged metadata information to: %s.\nError: %s", targetInfo, e.getMessage() ), e );
        }
        finally
        {
            closeQuietly( fw );
        }
    }

}
