package org.commonjava.aprox.depgraph.event;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.event.CartoEventManager;
import org.commonjava.maven.cartographer.event.CartoEventManagerImpl;
import org.commonjava.maven.cartographer.event.MissingRelationshipsEvent;
import org.commonjava.maven.cartographer.event.NewRelationshipsEvent;
import org.commonjava.maven.cartographer.event.ProjectRelationshipsErrorEvent;
import org.commonjava.maven.galley.event.FileErrorEvent;
import org.commonjava.maven.galley.event.FileNotFoundEvent;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class AproxDepgraphEvents
    implements CartoEventManager
{

    private final Logger logger = new Logger( getClass() );

    private CartoEventManagerImpl delegate;

    @Inject
    private Event<MissingRelationshipsEvent> missingEvents;

    protected AproxDepgraphEvents()
    {
    }

    public AproxDepgraphEvents( final CartoEventManagerImpl delegate )
    {
        this.delegate = delegate;
    }

    @PostConstruct
    public void setup()
    {
        delegate = new CartoEventManagerImpl();
    }

    public void unlockOnFileErrorEvent( @Observes final FileErrorEvent evt )
    {
        final String path = evt.getTransfer()
                               .getPath();
        try
        {
            final ArtifactPathInfo info = ArtifactPathInfo.parse( path );
            //            logger.info( "Unlocking %s due to file download error.", info );
            if ( info != null )
            {
                final ProjectVersionRef ref =
                    new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                delegate.notifyOfGraph( ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot parse version for path: '%s'. Failed to unlock waiting threads. Reason: %s", e, path,
                          e.getMessage() );
        }
    }

    public void unlockOnFileNotFoundEvent( @Observes final FileNotFoundEvent evt )
    {
        final String path = evt.getPath();
        try
        {
            final ArtifactPathInfo info = ArtifactPathInfo.parse( path );
            //            logger.info( "Unlocking %s due to unresolvable POM.", info );
            if ( info != null )
            {
                final ProjectVersionRef ref =
                    new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                delegate.notifyOfGraph( ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot parse version for path: '%s'. Failed to unlock waiting threads. Reason: %s", e, path,
                          e.getMessage() );
        }
    }

    @Override
    public void fireMissing( final MissingRelationshipsEvent missingRelationshipsEvent )
    {
        delegate.fireMissing( missingRelationshipsEvent );

        if ( missingEvents != null )
        {
            missingEvents.fire( missingRelationshipsEvent );
        }
    }

    @Override
    public void unlockOnNewRelationshipsEvent( final NewRelationshipsEvent evt )
    {
        delegate.unlockOnNewRelationshipsEvent( evt );
    }

    @Override
    public void unlockOnRelationshipsErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
        delegate.unlockOnRelationshipsErrorEvent( evt );
    }

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final CartoDataManager data, final long timeoutMillis )
        throws CartoDataException
    {
        delegate.waitForGraph( ref, data, timeoutMillis );
    }

}
