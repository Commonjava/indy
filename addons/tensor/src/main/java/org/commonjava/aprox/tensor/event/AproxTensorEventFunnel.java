package org.commonjava.aprox.tensor.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.common.version.InvalidVersionSpecificationException;
import org.commonjava.aprox.change.event.FileErrorEvent;
import org.commonjava.aprox.change.event.FileNotFoundEvent;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.tensor.event.AbstractTensorEventFunnel;

@ApplicationScoped
public class AproxTensorEventFunnel
    extends AbstractTensorEventFunnel
{

    public void unlockOnFileErrorEvent( @Observes final FileErrorEvent evt )
    {
        final String path = evt.getStorageItem()
                               .getPath();
        try
        {
            final ArtifactPathInfo info = ArtifactPathInfo.parse( path );
            logger.info( "Unlocking %s due to file download error.", info );
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
            logger.info( "Unlocking %s due to unresolvable POM.", info );
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

}
