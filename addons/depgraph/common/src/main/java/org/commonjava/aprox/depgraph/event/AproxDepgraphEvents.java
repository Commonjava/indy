/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
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
                final ProjectVersionRef ref =
                    new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                delegate.notifyOfGraph( ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( String.format( "Cannot parse version for path: '%s'. Failed to unlock waiting threads. Reason: %s",
                                         path, e.getMessage() ), e );
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
                final ProjectVersionRef ref =
                    new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                delegate.notifyOfGraph( ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( String.format( "Cannot parse version for path: '%s'. Failed to unlock waiting threads. Reason: %s",
                                         path, e.getMessage() ), e );
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
    public void waitForGraph( final ProjectVersionRef ref, final RelationshipGraph graph, final long timeoutMillis )
        throws CartoDataException
    {
        delegate.waitForGraph( ref, graph, timeoutMillis );
    }

}
