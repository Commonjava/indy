package org.commonjava.indy.data;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Move validation logic out of old ValidRemoteStoreDataManagerDecorator (in implied-repos) to here, so it's easier
 * to call it from more appropriate places while still reusing it.
 *
 * Created by jdcasey on 4/26/17.
 */
@ApplicationScoped
public class ArtifactStoreValidator
{
    @Inject
    private TransferManager transferManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected ArtifactStoreValidator(){}

    public ArtifactStoreValidator( TransferManager transferManager )
    {
        this.transferManager = transferManager;
    }

    public boolean isValid( ArtifactStore store )
    {
        if ( !( store instanceof RemoteRepository ) )
        {
            return true;
        }

        RemoteRepository remoteRepository = (RemoteRepository) store;
        URL url = null;
        try
        {
            url = new URL( remoteRepository.getUrl() );
            ConcreteResource concreteResource =
                    new ConcreteResource( LocationUtils.toLocation( remoteRepository ), PathUtils.ROOT );

            return transferManager.exists( concreteResource );
        }
        catch ( final MalformedURLException e )
        {
            logger.error( "[RemoteValidation] Failed to parse repository URL: '{}'. Reason: {}", e, url,
                          e.getMessage() );
        }
        catch ( final TransferException e )
        {
            logger.warn( "[RemoteValidation] Cannot connect to target repository: '{}'. Reason: {}", this,
                         e.getMessage() );
            logger.debug( "[RemoteValidation] exception from validation attempt for: {}", this, e );
        }

        return false;
    }
}
