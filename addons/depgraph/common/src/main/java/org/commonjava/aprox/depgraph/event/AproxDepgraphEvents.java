/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.event;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.event.CartoEventManager;
import org.commonjava.maven.cartographer.event.CartoEventManagerImpl;
import org.commonjava.maven.cartographer.event.ProjectRelationshipsErrorEvent;
import org.commonjava.maven.cartographer.event.RelationshipStorageEvent;
import org.commonjava.maven.galley.event.FileErrorEvent;
import org.commonjava.maven.galley.event.FileNotFoundEvent;
import org.commonjava.maven.galley.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Default
@Production
public class AproxDepgraphEvents
    implements CartoEventManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private CartoEventManagerImpl delegate;

    @Inject
    private Event<RelationshipStorageEvent> storageEvents;

    @Inject
    private Event<ProjectRelationshipsErrorEvent> errorEvents;

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
            //            logger.info( "Unlocking {} due to file download error.", info );
            if ( info != null )
            {
                final ProjectVersionRef ref = new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                delegate.notifyOfGraph( ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot parse version for path: '{}'. Failed to unlock waiting threads. Reason: {}", e, path, e.getMessage() );
        }
    }

    public void unlockOnFileNotFoundEvent( @Observes final FileNotFoundEvent evt )
    {
        final Resource resource = evt.getResource();
        final String path = resource.getPath();
        try
        {
            final ArtifactPathInfo info = ArtifactPathInfo.parse( path );
            //            logger.info( "Unlocking {} due to unresolvable POM.", info );
            if ( info != null )
            {
                final ProjectVersionRef ref = new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                delegate.notifyOfGraph( ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Cannot parse version for path: '{}'. Failed to unlock waiting threads. Reason: {}", e, path, e.getMessage() );
        }
    }

    @Override
    public void fireStorageEvent( final RelationshipStorageEvent evt )
    {
        delegate.fireStorageEvent( evt );
        if ( storageEvents != null )
        {
            storageEvents.fire( evt );
        }
    }

    @Override
    public void fireErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
        delegate.fireErrorEvent( evt );
        if ( errorEvents != null )
        {
            errorEvents.fire( evt );
        }
    }

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final CartoDataManager data, final long timeoutMillis )
        throws CartoDataException
    {
        delegate.waitForGraph( ref, data, timeoutMillis );
    }

}
