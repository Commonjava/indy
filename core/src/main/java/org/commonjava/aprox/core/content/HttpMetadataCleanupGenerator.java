package org.commonjava.aprox.core.content;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentGenerator;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMetadataCleanupGenerator
    implements ContentGenerator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DownloadManager downloads;

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> generateDirectoryContent( final ArtifactStore store, final String path,
                                                         final List<StoreResource> existing,
                                                         final EventMetadata eventMetadata )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public Transfer generateGroupFileContent( final Group group, final List<ArtifactStore> members, final String path,
                                              final EventMetadata eventMetadata )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> generateGroupDirectoryContent( final Group group, final List<ArtifactStore> members,
                                                              final String path, final EventMetadata eventMetadata )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public void handleContentStorage( final ArtifactStore store, final String path, final Transfer result,
                                      final EventMetadata eventMetadata )
        throws AproxWorkflowException
    {
        final Transfer meta = result.getSiblingMeta( HttpExchangeMetadata.FILE_EXTENSION );
        if ( meta.exists() )
        {
            try
            {
                meta.delete( false );
            }
            catch ( final IOException e )
            {
                logger.debug( "Failed to delete HTTP exchange metadata: " + meta, e );
            }
        }
    }

    @Override
    public void handleContentDeletion( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
        throws AproxWorkflowException
    {
        final Transfer meta = downloads.getStorageReference( store, path + HttpExchangeMetadata.FILE_EXTENSION );
        if ( meta.exists() )
        {
            try
            {
                meta.delete( false );
            }
            catch ( final IOException e )
            {
                logger.debug( "Failed to delete HTTP exchange metadata: " + meta, e );
            }
        }
    }

}
