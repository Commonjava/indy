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

import java.net.URI;
import java.util.Collections;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
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
    private AproxModelDiscoverer discoverer;

    @Inject
    private ArtifactManager artifactManager;

    @Inject
    private CartoDataManager dataManager;

    protected AproxProjectGraphDiscoverer()
    {
    }

    public AproxProjectGraphDiscoverer( final AproxModelDiscoverer discoverer, final ArtifactManager artifactManager,
                                        final CartoDataManager dataManager )
    {
        this.discoverer = discoverer;
        this.artifactManager = artifactManager;
        this.dataManager = dataManager;
    }

    /**
     * @deprecated Use {@link #discoverRelationships(ProjectVersionRef,DiscoveryConfig)} instead
     */
    @Deprecated
    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig, final boolean storeRelationships )
        throws CartoDataException
    {
        discoveryConfig.setStoreRelationships( storeRelationships );
        return discoverRelationships( ref, discoveryConfig );
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final URI source = discoveryConfig.getDiscoverySource();

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
            dataManager.addError( new EProjectKey( source, ref ), e );
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
                return discoverer.discoverRelationships( specific, retrieved, discoveryConfig );
            }
            else
            {
                logger.debug( "{} NOT FOUND in:\n  {}", pomRef, new JoinString( "\n  ", discoveryConfig.getLocations() ) );
                return null;
            }
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Discovery of project-relationships for: '{}' failed. Error: {}", e, ref, e.getMessage() );
        }
    }

    private void setLocation( final DiscoveryConfig discoveryConfig )
    {
        synchronized ( discoveryConfig )
        {
            if ( discoveryConfig.getLocations() == null )
            {
                final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );
                final Location location = LocationUtils.toCacheLocation( key );
                discoveryConfig.setLocations( Collections.singletonList( location ) );
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
            throw new CartoDataException( "Failed to resolve variable version for: {}. Reason: {}", e, ref, e.getMessage() );
        }
    }

}
