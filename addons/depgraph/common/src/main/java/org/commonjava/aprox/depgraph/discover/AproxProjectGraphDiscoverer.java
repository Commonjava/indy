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
package org.commonjava.aprox.depgraph.discover;

import java.util.Collections;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
@Production
@Default
public class AproxProjectGraphDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected AproxModelDiscoverer discoverer;

    @Inject
    protected ArtifactManager artifactManager;

    @Inject
    protected StoreDataManager storeManager;

    protected AproxProjectGraphDiscoverer()
    {
    }

    public AproxProjectGraphDiscoverer( final AproxModelDiscoverer discoverer, final ArtifactManager artifactManager )
    {
        this.discoverer = discoverer;
        this.artifactManager = artifactManager;
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final RelationshipGraph graph,
                                                  final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        ProjectVersionRef specific = ref;
        try
        {
            if ( !ref.isSpecificVersion() )
            {
                specific = resolveSpecificVersion( ref, discoveryConfig );
                if ( specific == null || specific.equals( ref ) )
                {
                    logger.warn( "Cannot resolve specific version of: '{}'.", ref );
                    return null;
                }
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( String.format( "Invalid version for: %s. Reason: %s", ref, e.getMessage() ), e );
            try
            {
                graph.storeProjectError( ref, e );
            }
            catch ( final RelationshipGraphException storeError )
            {
                logger.error( String.format( "Failed to store error for project: %s in graph: %s. Reason: %s", ref,
                                             graph, e.getMessage() ), e );
            }

            specific = null;
        }

        if ( specific == null )
        {
            logger.warn( "Specific version NOT resolved. Skipping discovery: {}", ref );
            return null;
        }

        setLocation( discoveryConfig );

        try
        {
            final ArtifactRef pomRef = specific.asPomArtifact();

            final Transfer retrieved;

            retrieved = artifactManager.retrieveFirst( discoveryConfig.getLocations(), pomRef );

            if ( retrieved != null )
            {
                return discoverer.discoverRelationships( specific, retrieved, graph, discoveryConfig );
            }
            else
            {
                logger.debug( "{} NOT FOUND in:\n  {}", pomRef, new JoinString( "\n  ", discoveryConfig.getLocations() ) );
                return null;
            }
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Discovery of project-relationships for: '{}' failed. Error: {}", e, ref,
                                          e.getMessage() );
        }
    }

    private void setLocation( final DiscoveryConfig discoveryConfig )
    {
        synchronized ( discoveryConfig )
        {
            if ( discoveryConfig.getLocations() == null )
            {
                final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );
                ArtifactStore store = null;
                try
                {
                    store = storeManager.getArtifactStore( key );
                }
                catch ( final AproxDataException e )
                {
                    logger.error( String.format( "Failed to lookup ArtifactStore for key: {}. Reason: {}", key,
                                                 e.getMessage() ), e );
                }

                if ( store != null )
                {
                    final Location location = LocationUtils.toLocation( store );
                    discoveryConfig.setLocations( Collections.singletonList( location ) );
                }
            }
        }
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        setLocation( discoveryConfig );

        try
        {
            return artifactManager.resolveVariableVersion( discoveryConfig.getLocations(), ref );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve variable version for: {}. Reason: {}", e, ref,
                                          e.getMessage() );
        }
    }

}
