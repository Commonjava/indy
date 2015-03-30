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
package org.commonjava.aprox.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.GroupLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.model.galley.RepositoryLocation;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Resource;
import org.commonjava.maven.galley.model.VirtualResource;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LocationExpander} implementation geared to work with {@link ArtifactStore} instances. This is responsible for resolving group references
 * into collections of concrete store references, for the purposes of resolving content. Via its use of {@link LocationUtils}, it also is responsible
 * for creating {@link Location} instances for remote repositories that contain relevant authentication and SSL attributes.
 */
@ApplicationScoped
public class AproxLocationExpander
    implements LocationExpander
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager data;

    protected AproxLocationExpander()
    {
    }

    public AproxLocationExpander( final StoreDataManager data )
    {
        this.data = data;
    }

    /**
     * @see AproxLocationExpander#expand(Collection)
     */
    @Override
    public List<Location> expand( final Location... locations )
        throws TransferException
    {
        return expand( Arrays.asList( locations ) );
    }

    /**
     * For group references, expand into a list of concrete repository locations (hosted or remote). For remote repository references that are 
     * specified as cache-only locations (see: {@link CacheOnlyLocation}), lookup the corresponding {@link RemoteRepository} and use it to create a
     * {@link RepositoryLocation} that contains any relevant SSL, authentication, proxy, etc. attbributes.
     */
    @Override
    public <T extends Location> List<Location> expand( final Collection<T> locations )
        throws TransferException
    {
        final List<Location> result = new ArrayList<Location>();
        for ( final Location location : locations )
        {
            if ( location instanceof GroupLocation )
            {
                final GroupLocation gl = (GroupLocation) location;
                try
                {
                    logger.debug( "Expanding group: {}", gl.getKey() );
                    final List<ArtifactStore> members = data.getOrderedConcreteStoresInGroup( gl.getKey()
                                                                                                .getName() );
                    if ( members != null )
                    {
                        for ( final ArtifactStore member : members )
                        {
                            if ( !result.contains( member ) )
                            {
                                logger.debug( "expansion += {}", member.getKey() );
                                result.add( LocationUtils.toLocation( member ) );
                            }
                        }
                        logger.debug( "Expanded group: {} to:\n  {}", gl.getKey(), new JoinString( "\n  ", result ) );
                    }
                }
                catch ( final AproxDataException e )
                {
                    throw new TransferException(
                                                 "Failed to lookup ordered concrete artifact stores contained in group: {}. Reason: {}",
                                                 e, gl, e.getMessage() );
                }
            }
            else if ( location instanceof CacheOnlyLocation && !( (CacheOnlyLocation) location ).isHostedRepository() )
            {
                final StoreKey key = ( (KeyedLocation) location ).getKey();
                try
                {
                    final ArtifactStore store = data.getArtifactStore( key );

                    if ( store == null )
                    {
                        throw new TransferException( "Cannot find ArtifactStore to match key: %s.", key );
                    }

                    logger.debug( "Adding single store: {} for location: {}", store, location );
                    result.add( LocationUtils.toLocation( store ) );
                }
                catch ( final AproxDataException e )
                {
                    throw new TransferException( "Failed to lookup store for key: {}. Reason: {}", e, key,
                                                 e.getMessage() );
                }
            }
            else
            {
                logger.debug( "No expansion available for location: {}", location );
                result.add( location );
            }
        }

        return result;
    }

    /** Using the same basic logic as {@link AproxLocationExpander#expand(Collection)}, convert the specified {@link Resource} into a 
     * {@link VirtualResource} that contains references to the expanded {@link Location} list.
     * 
     * @see AproxLocationExpander#expand(Collection)
     */
    @Override
    public VirtualResource expand( final Resource resource )
        throws TransferException
    {
        List<Location> locations;
        if ( resource instanceof VirtualResource )
        {
            final List<ConcreteResource> concrete = ( (VirtualResource) resource ).toConcreteResources();
            final List<ConcreteResource> result = new ArrayList<ConcreteResource>();
            for ( final ConcreteResource cr : concrete )
            {
                final List<Location> expanded = expand( cr.getLocation() );
                for ( final Location location : expanded )
                {
                    result.add( new ConcreteResource( location, cr.getPath() ) );
                }
            }

            return new VirtualResource( result );
        }
        else
        {
            final ConcreteResource cr = (ConcreteResource) resource;
            locations = expand( cr.getLocation() );

            return new VirtualResource( locations, cr.getPath() );
        }
    }

}
