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

    public static final String GROUP_METADATA_GENERATED = "group-metadata-generated";

    public static final String GROUP_METADATA_EXISTS = "group-metadata-exists";

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
        else
        {
            logger.trace( "{} does not exist. Not deleting.", targetSha );
        }

        if ( targetMd5 != null )
        {
            logger.debug( "Deleting: {}", targetMd5 );
            targetMd5.delete();
        }
        else
        {
            logger.trace( "{} does not exist. Not deleting.", targetMd5 );
        }

        if ( targetInfo != null )
        {
            logger.debug( "Deleting: {}", targetInfo );
            targetInfo.delete();
        }
        else
        {
            logger.trace( "{} does not exist. Not deleting.", targetInfo );
        }
    }

    @Deprecated
    public final void writeMergeInfo( final byte[] data, final List<Transfer> sources, final Group group,
                                      final String path )
    {
        writeMergeInfo( generateMergeInfo( sources ), group, path );
    }

    public final String generateMergeInfo( final List<Transfer> sources )
    {
        final StringBuilder mergeInfoBuilder = new StringBuilder();
        for ( final Transfer source : sources )
        {
            mergeInfoBuilder.append( getKey( source ).toString() );
            mergeInfoBuilder.append( "\n" );
        }
        return mergeInfoBuilder.toString();
    }

    public final String generateMergeInfoFromKeys( final List<StoreKey> sources )
    {
        final StringBuilder mergeInfoBuilder = new StringBuilder();
        sources.forEach( src->mergeInfoBuilder.append(src).append('\n') );
        return mergeInfoBuilder.toString();
    }

    public final void writeMergeInfo( final String mergeInfo, final Group group, final String path )
    {
        final String infoPath = path+MERGEINFO_SUFFIX;
        logger.trace( ".info file path is {} for group {} (members: {}), content is {}", infoPath, group.getKey(),
                      group.getConstituents(), mergeInfo );

        final Transfer targetInfo = downloadManager.getStorageReference( group, infoPath );
        Writer fw = null;
        try
        {
            fw = new OutputStreamWriter( targetInfo.openOutputStream( TransferOperation.GENERATE ) );
            fw.write( mergeInfo );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to write merged metadata information to: %s.\nError: %s", targetInfo,
                                         e.getMessage() ), e );
        }
        finally
        {
            closeQuietly( fw );
        }
    }

}
