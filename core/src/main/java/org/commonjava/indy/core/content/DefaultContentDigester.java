package org.commonjava.indy.core.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.core.inject.ContentMetadataCache;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.io.checksum.ContentDigest;
import org.commonjava.maven.galley.io.checksum.TransferMetadata;
import org.commonjava.maven.galley.model.Transfer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.codec.binary.Hex.encodeHexString;

/**
 * Created by jdcasey on 1/4/17.
 * Migrated out of DefaultContentManager so it can be used from other places. This isn't really specific to the
 * {@link org.commonjava.indy.content.ContentManager} interface anyway.
 */
@ApplicationScoped
public class DefaultContentDigester
        implements ContentDigester

{

    @Inject
    private DirectContentAccess directContentAccess;

    @Inject
    @ContentMetadataCache
    private CacheHandle<String, TransferMetadata> metadataCache;

    protected DefaultContentDigester()
    {
    }

    public DefaultContentDigester( DirectContentAccess directContentAccess,
                                   CacheHandle<String, TransferMetadata> metadataCache )
    {
        this.directContentAccess = directContentAccess;
        this.metadataCache = metadataCache;
    }

    @Override
    public synchronized void accept( final Transfer transfer, final TransferMetadata transferData )
    {
        KeyedLocation kl = (KeyedLocation) transfer.getLocation();
        metadataCache.put( kl.getKey() + "#" + transfer.getPath(), transferData );
    }

    @Override
    public synchronized TransferMetadata getContentMetadata( final Transfer transfer )
    {
        KeyedLocation kl = (KeyedLocation) transfer.getLocation();
        return metadataCache.get( kl.getKey() + "#" + transfer.getPath() );
    }

    public TransferMetadata digest( final StoreKey key, final String path, final ContentDigest... types )
            throws IndyWorkflowException
    {
        final Transfer txfr = directContentAccess.getTransfer( key, path );
        if ( txfr == null || !txfr.exists() )
        {
            return new TransferMetadata( Collections.emptyMap(), 0L );
        }

        TransferMetadata meta = getContentMetadata( txfr );
        if ( meta != null )
        {
            return meta;
        }

        InputStream stream = null;
        try
        {
            long artifactSize = 0L;
            // TODO: Compute it as the file is uploaded/downloaded into cache.
            stream = txfr.openInputStream( false );

            final Map<ContentDigest, MessageDigest> digests = new HashMap<>();
            for ( final ContentDigest digest : types )
            {
                digests.put( digest, MessageDigest.getInstance( digest.digestName() ) );
            }

            final byte[] buf = new byte[16384];
            int read = -1;
            while ( ( read = stream.read( buf ) ) > -1 )
            {
                for ( final MessageDigest digest : digests.values() )
                {
                    digest.update( buf, 0, read );
                }
                artifactSize += read;
            }

            final Map<ContentDigest, String> digestResultMap = new HashMap<>();
            for ( final Map.Entry<ContentDigest, MessageDigest> entry : digests.entrySet() )
            {
                digestResultMap.put( entry.getKey(), encodeHexString( entry.getValue().digest() ) );
            }

            meta = new TransferMetadata( digestResultMap, artifactSize );
            accept( txfr, meta );

            return meta;
        }
        catch ( IOException | NoSuchAlgorithmException e )
        {
            throw new IndyWorkflowException( "Failed to calculate checksums (MD5, SHA-256, etc.) for: %s. Reason: %s",
                                             e, txfr, e.getMessage() );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }
}
