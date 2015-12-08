/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.depgraph.discover;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.depgraph.util.IndyDepgraphUtils;
import org.commonjava.indy.inject.Production;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.cartographer.spi.graph.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.Collections;

@javax.enterprise.context.ApplicationScoped
@Production
@Default
public class IndyProjectGraphDiscoverer
    implements ProjectRelationshipDiscoverer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected IndyModelDiscoverer discoverer;

    @Inject
    protected ArtifactManager artifactManager;

    @Inject
    protected StoreDataManager storeManager;

    protected IndyProjectGraphDiscoverer()
    {
    }

    public IndyProjectGraphDiscoverer( final IndyModelDiscoverer discoverer, final ArtifactManager artifactManager )
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
                    logger.warn( "Cannot graph specific version of: '{}'.", ref );
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

            retrieved = artifactManager.retrieveFirst( discoveryConfig.getLocations(), pomRef, new EventMetadata() );

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
                final StoreKey key = IndyDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );
                ArtifactStore store = null;
                try
                {
                    store = storeManager.getArtifactStore( key );
                }
                catch ( final IndyDataException e )
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
            return artifactManager.resolveVariableVersion( discoveryConfig.getLocations(), ref, new EventMetadata() );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to graph variable version for: {}. Reason: {}", e, ref,
                                          e.getMessage() );
        }
    }

}
