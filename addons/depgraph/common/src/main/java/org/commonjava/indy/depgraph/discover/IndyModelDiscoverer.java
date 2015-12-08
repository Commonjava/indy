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

import org.commonjava.indy.depgraph.util.IndyDepgraphUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.cartographer.graph.MavenModelProcessor;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.cartographer.graph.discover.meta.MetadataScannerSupport;
import org.commonjava.cartographer.graph.discover.patch.PatcherSupport;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class IndyModelDiscoverer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected MavenModelProcessor modelProcessor;

    @Inject
    protected PatcherSupport patchers;

    @Inject
    protected MetadataScannerSupport metadataScannerSupport;

    @Inject
    protected MavenPomReader pomReader;

    protected IndyModelDiscoverer()
    {
    }

    public IndyModelDiscoverer( final MavenModelProcessor modelProcessor, final PatcherSupport patchers,
                                 final MetadataScannerSupport metadataScannerSupport )
    {
        this.modelProcessor = modelProcessor;
        this.patchers = patchers;
        this.metadataScannerSupport = metadataScannerSupport;
    }

    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final Transfer item,
                                                  final RelationshipGraph graph, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final String path = item.getPath();

        if ( !path.endsWith( ".pom" ) )
        {
            logger.debug( "NOT a POM: {}", path );
            return null;
        }

        final StoreKey key = LocationUtils.getKey( item );

        final URI source = IndyDepgraphUtils.toDiscoveryURI( key );

        final List<? extends Location> locations = discoveryConfig.getLocations();
        final Collection<String> enabledPatchers = discoveryConfig.getEnabledPatchers();
        final boolean storeRelationships = discoveryConfig.isStoreRelationships();

        MavenPomView pomView;
        try
        {
            pomView = pomReader.read( ref, item, locations );
        }
        catch ( final GalleyMavenException e )
        {
            throw new CartoDataException( "Failed to parse: {}. Reason: {}", e, item, e.getMessage() );
        }

        DiscoveryResult result = modelProcessor.readRelationships( pomView, source, discoveryConfig );

        if ( result != null )
        {
            logger.info( "Attempting to patch {} results for: {}", result.getAcceptedRelationships()
                                                                         .size(), result.getSelectedRef() );
            result = patchers.patch( result, enabledPatchers, locations, pomView, item );
            logger.info( "After patching, {} relationships were discovered.", result.getAcceptedRelationships()
                                                                                    .size() );

            final Map<String, String> metadata =
                metadataScannerSupport.scan( result.getSelectedRef(), locations, pomView, item );
            result.setMetadata( metadata );

            if ( storeRelationships )
            {
                logger.info( "Storing discovered relationships." );
                try
                {
                    final Set<ProjectRelationship<?, ?>> rejected =
                        graph.storeRelationships( result.getAcceptedRelationships() );

                    graph.addMetadata( result.getSelectedRef(), metadata );

                    result = new DiscoveryResult( result.getSource(), result, rejected );
                }
                catch ( final RelationshipGraphException e )
                {
                    throw new CartoDataException(
                                                  "Failed to store parsed relationships or metadata for: {}. Reason: {}",
                                                  e, ref, e.getMessage() );
                }
            }
            else
            {
                logger.info( "NOT storing discovered relationships." );
            }
        }

        return result;
    }

}
