/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.content;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.galley.CacheOnlyLocation;
import org.commonjava.indy.model.galley.GroupLocation;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.atlas.maven.ident.util.JoinString;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Resource;
import org.commonjava.maven.galley.model.VirtualResource;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * {@link LocationExpander} implementation geared to work with {@link ArtifactStore} instances. This is responsible for resolving group references
 * into collections of concrete store references, for the purposes of resolving content. Via its use of {@link LocationUtils}, it also is responsible
 * for creating {@link Location} instances for remote repositories that contain relevant authentication and SSL attributes.
 */
public class IndyLocationExpander
    implements LocationExpander
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager data;

    protected IndyLocationExpander()
    {
    }

    public IndyLocationExpander( final StoreDataManager data )
    {
        this.data = data;
    }

    /**
     * @see IndyLocationExpander#expand(Collection)
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
                    final List<ArtifactStore> members =
                            data.query().packageType( gl.getKey().getPackageType() ).getOrderedConcreteStoresInGroup( gl.getKey().getName() );

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
                catch ( final IndyDataException e )
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
                catch ( final IndyDataException e )
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

    /** Using the same basic logic as {@link IndyLocationExpander#expand(Collection)}, convert the specified {@link Resource} into a
     * {@link VirtualResource} that contains references to the expanded {@link Location} list.
     * 
     * @see IndyLocationExpander#expand(Collection)
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
