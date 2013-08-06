package org.commonjava.aprox.depgraph.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.event.AbstractCartoEventManager;
import org.commonjava.maven.cartographer.event.MissingRelationshipsEvent;
import org.commonjava.maven.galley.event.FileErrorEvent;
import org.commonjava.maven.galley.event.FileNotFoundEvent;

@ApplicationScoped
public class AproxEventFunnel
    extends AbstractCartoEventManager
{

    @Inject
    private Event<MissingRelationshipsEvent> missingEvents;

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
                notifyOfGraph( ref );
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
                notifyOfGraph( ref );
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
        if ( missingEvents != null )
        {
            missingEvents.fire( missingRelationshipsEvent );
        }
    }

}
