package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class CacheFirstTimeoutWorkingTest
        extends AbstractMetadataTimeoutWorkingTest
{
    private static final int METADATA_TIMEOUT_SECONDS = 7;

    private static final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 5000;

    private static final int CACHE_TIMEOUT_SECONDS = 2;

    private static final int CACHE_TIMEOUT_WAITING_MILLISECONDS = 3000;

    @Test
    public void timeout()
            throws Exception
    {
        // make sure the metadata timout
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", CACHE_TIMEOUT_SECONDS );
        final File pomFileAgain = new File( pomFilePath );
        assertThat( "artifact should be removed when cache timeout", pomFileAgain.exists(), equalTo( false ) );
        File metadataFileAgain = new File( metadataFilePath );
        assertThat( "metadata should not be removed when cache timeout", metadataFileAgain.exists(), equalTo( true ) );
        File archetypeFileAgain = new File( archetypeFilePath );
        assertThat( "archetype should not be removed when cache timeout", archetypeFileAgain.exists(),
                    equalTo( true ) );

        // make sure the repo timout
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", METADATA_TIMEOUT_SECONDS );
        metadataFileAgain = new File( metadataFilePath );
        assertThat( "metadata should be removed when metadata timeout", metadataFileAgain.exists(), equalTo( false ) );
        archetypeFileAgain = new File( archetypeFilePath );
        assertThat( "archetype should be removed when metadata timeout", archetypeFileAgain.exists(), equalTo( false ) );
    }

    @Override
    protected RemoteRepository createRemoteRepository()
    {
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setCacheTimeoutSeconds( CACHE_TIMEOUT_SECONDS );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        return repository;
    }
}
