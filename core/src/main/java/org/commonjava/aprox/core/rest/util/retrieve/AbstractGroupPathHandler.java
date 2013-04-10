package org.commonjava.aprox.core.rest.util.retrieve;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.aprox.change.event.FileDeletionEvent;
import org.commonjava.aprox.change.event.FileEventManager;
import org.commonjava.aprox.change.event.FileStorageEvent;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.rest.util.retrieve.GroupPathHandler;
import org.commonjava.util.logging.Logger;

public abstract class AbstractGroupPathHandler
    implements GroupPathHandler
{

    protected final Logger logger = new Logger( getClass() );

    @Inject
    protected FileManager fileManager;

    @Inject
    protected FileEventManager fileEvent;

    protected final void deleteChecksumsAndMergeInfo( final Group group, final String path )
        throws IOException
    {
        final StorageItem targetSha = fileManager.getStorageReference( group, path + SHA_SUFFIX );
        final StorageItem targetMd5 = fileManager.getStorageReference( group, path + MD5_SUFFIX );
        final StorageItem targetInfo = fileManager.getStorageReference( group, path + MERGEINFO_SUFFIX );

        if ( targetSha != null )
        {
            targetSha.delete();

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileDeletionEvent( targetSha ) );
            }
        }

        if ( targetMd5 != null )
        {
            targetMd5.delete();

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileDeletionEvent( targetMd5 ) );
            }
        }

        if ( targetInfo != null )
        {
            targetInfo.delete();

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileDeletionEvent( targetInfo ) );
            }
        }
    }

    protected final void writeChecksumsAndMergeInfo( final byte[] data, final Set<StorageItem> sources,
                                                     final Group group, final String path )
    {
        final StorageItem targetSha = fileManager.getStorageReference( group, path + ".sha" );
        final StorageItem targetMd5 = fileManager.getStorageReference( group, path + ".md5" );
        final StorageItem targetInfo = fileManager.getStorageReference( group, path + MERGEINFO_SUFFIX );

        final String sha = shaHex( data );
        final String md5 = md5Hex( data );

        Writer fw = null;
        try
        {
            fw = new OutputStreamWriter( targetSha.openOutputStream( true ) );
            fw.write( sha );

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.GENERATE, targetSha ) );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to write SHA1 checksum for merged metadata information to: %s.\nError: %s", e,
                          targetSha, e.getMessage() );
        }
        finally
        {
            closeQuietly( fw );
        }

        try
        {
            fw = new OutputStreamWriter( targetMd5.openOutputStream( true ) );
            fw.write( md5 );

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.GENERATE, targetMd5 ) );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to write MD5 checksum for merged metadata information to: %s.\nError: %s", e,
                          targetMd5, e.getMessage() );
        }
        finally
        {
            closeQuietly( fw );
        }

        try
        {
            fw = new OutputStreamWriter( targetInfo.openOutputStream() );
            for ( final StorageItem source : sources )
            {
                fw.write( source.getStoreKey()
                                .toString() );
                fw.write( "\n" );
            }

            if ( fileEvent != null )
            {
                fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.GENERATE, targetInfo ) );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to write merged metadata information to: %s.\nError: %s", e, targetInfo,
                          e.getMessage() );
        }
        finally
        {
            closeQuietly( fw );
        }
    }

}
