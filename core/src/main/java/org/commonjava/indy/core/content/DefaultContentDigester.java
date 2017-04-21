package org.commonjava.indy.core.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentDigester;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.core.inject.ContentMetadataCache;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.ChecksummingTransferDecorator;
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
import static org.commonjava.maven.galley.io.ChecksummingTransferDecorator.FORCE_CHECKSUM;

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
    public synchronized void addMetadata( final Transfer transfer, final TransferMetadata transferData )
    {
        KeyedLocation kl = (KeyedLocation) transfer.getLocation();
        metadataCache.put( kl.getKey() + "#" + transfer.getPath(), transferData );
    }

    @Override
    public synchronized void removeMetadata( final Transfer transfer )
    {
        KeyedLocation kl = (KeyedLocation) transfer.getLocation();
        metadataCache.remove( kl.getKey() + "#" + transfer.getPath() );
    }

    @Override
    public synchronized TransferMetadata getContentMetadata( final Transfer transfer )
    {
        KeyedLocation kl = (KeyedLocation) transfer.getLocation();
        return metadataCache.get( kl.getKey() + "#" + transfer.getPath() );
    }

    public TransferMetadata digest( final StoreKey key, final String path, final EventMetadata eventMetadata,
                                    final ContentDigest... types )
            throws IndyWorkflowException
    {
        final Transfer transfer = directContentAccess.getTransfer( key, path );
        if ( transfer == null || !transfer.exists() )
        {
            return new TransferMetadata( Collections.emptyMap(), 0L );
        }

        TransferMetadata meta = getContentMetadata( transfer );
        if ( meta != null )
        {
            return meta;
        }

        EventMetadata forcedEventMetadata = new EventMetadata( eventMetadata ).set( FORCE_CHECKSUM, Boolean.TRUE );
        try(InputStream stream = transfer.openInputStream( false, forcedEventMetadata ) )
        {
            // depend on ChecksummingTransferDecorator to calculate / store metadata as this gets read, using
            // the FORCE_CHECKSUM metadata key to control its generation.
            int read = -1;
            byte[] buf = new byte[16];
            while ( ( read = stream.read( buf ) ) > -1 )
            {
                // NOP
            }
        }
        catch ( IOException e )
        {
            throw new IndyWorkflowException( "Failed to calculate checksums (MD5, SHA-256, etc.) for: %s. Reason: %s",
                                             e, transfer, e.getMessage() );
        }

        return getContentMetadata( transfer );
    }
}
