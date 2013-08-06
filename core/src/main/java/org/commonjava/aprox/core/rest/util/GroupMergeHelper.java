package org.commonjava.aprox.core.rest.util;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.util.retrieve.GroupPathHandler;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.util.logging.Logger;

public class GroupMergeHelper
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private FileManager fileManager;

    public final void deleteChecksumsAndMergeInfo( final Group group, final String path )
        throws IOException
    {
        final Transfer targetSha = fileManager.getStorageReference( group, path + GroupPathHandler.SHA_SUFFIX );
        final Transfer targetMd5 = fileManager.getStorageReference( group, path + GroupPathHandler.MD5_SUFFIX );
        final Transfer targetInfo = fileManager.getStorageReference( group, path + GroupPathHandler.MERGEINFO_SUFFIX );

        if ( targetSha != null )
        {
            targetSha.delete();
        }

        if ( targetMd5 != null )
        {
            targetMd5.delete();
        }

        if ( targetInfo != null )
        {
            targetInfo.delete();
        }
    }

    public final void writeChecksumsAndMergeInfo( final byte[] data, final Set<Transfer> sources, final Group group,
                                                  final String path )
    {
        final Transfer targetSha = fileManager.getStorageReference( group, path + GroupPathHandler.SHA_SUFFIX );
        final Transfer targetMd5 = fileManager.getStorageReference( group, path + GroupPathHandler.MD5_SUFFIX );
        final Transfer targetInfo = fileManager.getStorageReference( group, path + GroupPathHandler.MERGEINFO_SUFFIX );

        final String sha = shaHex( data );
        final String md5 = md5Hex( data );

        Writer fw = null;
        try
        {
            fw = new OutputStreamWriter( targetSha.openOutputStream( TransferOperation.GENERATE, true ) );
            fw.write( sha );
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
            fw = new OutputStreamWriter( targetMd5.openOutputStream( TransferOperation.GENERATE, true ) );
            fw.write( md5 );
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
            logger.error( "Failed to write merged metadata information to: %s.\nError: %s", e, targetInfo,
                          e.getMessage() );
        }
        finally
        {
            closeQuietly( fw );
        }
    }

}
