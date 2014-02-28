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
package org.commonjava.aprox.depgraph.discover;

import java.net.URI;
import java.util.List;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.StringFormat;
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

    @Inject
    private StoreDataManager storeManager;

    protected AproxProjectGraphDiscoverer()
    {
    }

    public AproxProjectGraphDiscoverer( final AproxModelDiscoverer discoverer, final ArtifactManager artifactManager,
                                        final CartoDataManager dataManager, final StoreDataManager storeManager )
    {
        this.discoverer = discoverer;
        this.artifactManager = artifactManager;
        this.dataManager = dataManager;
        this.storeManager = storeManager;
    }

    @Override
    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig, final boolean storeRelationships )
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
            logger.error( "{}", e, new StringFormat( "Invalid version for: {}. Reason: {}", ref, e.getMessage() ) );
            dataManager.addError( new EProjectKey( source, ref ), e );
            specific = null;
        }

        if ( specific == null )
        {
            logger.warn( "Specific version NOT resolved. Skipping discovery: {}", ref );
            return null;
        }

        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );

        try
        {
            final ArtifactRef pomRef = specific.asPomArtifact();

            final Transfer retrieved;
            final List<? extends KeyedLocation> locations = getLocations( key );
            if ( locations == null || locations.isEmpty() )
            {
                logger.warn( "NO LOCATIONS given for resolving: {}", pomRef );
                return null;
            }

            retrieved = artifactManager.retrieveFirst( locations, pomRef );

            if ( retrieved != null )
            {
                return discoverer.discoverRelationships( specific, retrieved, locations, discoveryConfig.getEnabledPatchers(), storeRelationships );
            }
            else
            {
                logger.debug( "{} NOT FOUND in:\n  {}", pomRef, new JoinString( "\n  ", locations ) );
                return null;
            }
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Discovery of project-relationships for: '{}' failed. Error: {}", e, ref, e.getMessage() );
        }
    }

    private List<? extends KeyedLocation> getLocations( final StoreKey key )
        throws CartoDataException
    {
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new CartoDataException( "Failed to lookup ArtifactStore for key: {}. Reason: {}", e, key, e.getMessage() );
        }

        List<? extends KeyedLocation> locations;
        if ( key == null )
        {
            try
            {
                locations = LocationUtils.toLocations( storeManager.getAllConcreteArtifactStores() );
            }
            catch ( final ProxyDataException e )
            {
                throw new CartoDataException( "Cannot retrieve full list of non-group artifact stores. Reason: {}", e, e.getMessage() );
            }
        }
        else if ( store == null )
        {
            throw new CartoDataException( "Cannot discover depgraphs from: {}. No such store.", key );
        }
        else if ( key.getType() == StoreType.group )
        {
            List<ArtifactStore> concrete;
            try
            {
                concrete = storeManager.getOrderedConcreteStoresInGroup( key.getName() );
            }
            catch ( final ProxyDataException e )
            {
                throw new CartoDataException( "Failed to lookup ordered list of concrete ArtifactStores for group: {}. Reason: {}", e, key,
                                              e.getMessage() );
            }

            locations = LocationUtils.toLocations( concrete );
        }
        else
        {
            locations = LocationUtils.toLocations( store );
        }

        return locations;
    }

    @Override
    public ProjectVersionRef resolveSpecificVersion( final ProjectVersionRef ref, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( discoveryConfig.getDiscoverySource() );
        final List<? extends KeyedLocation> locations = getLocations( key );

        try
        {
            return artifactManager.resolveVariableVersion( locations, ref );
        }
        catch ( final TransferException e )
        {
            throw new CartoDataException( "Failed to resolve variable version for: {}. Reason: {}", e, ref, e.getMessage() );
        }
    }

}
