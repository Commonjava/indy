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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.util.AproxDepgraphUtils;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AproxModelDiscoverer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MavenModelProcessor modelProcessor;

    @Inject
    private CartoDataManager dataManager;

    @Inject
    private PatcherSupport patchers;

    @Inject
    private MetadataScannerSupport metadataScannerSupport;

    @Inject
    private MavenPomReader pomReader;

    protected AproxModelDiscoverer()
    {
    }

    public AproxModelDiscoverer( final MavenModelProcessor modelProcessor, final CartoDataManager dataManager, final PatcherSupport patchers,
                                 final MetadataScannerSupport metadataScannerSupport )
    {
        this.modelProcessor = modelProcessor;
        this.dataManager = dataManager;
        this.patchers = patchers;
        this.metadataScannerSupport = metadataScannerSupport;
    }

    public DiscoveryResult discoverRelationships( final ProjectVersionRef ref, final Transfer item, final DiscoveryConfig discoveryConfig )
        throws CartoDataException
    {
        final String path = item.getPath();

        if ( !path.endsWith( ".pom" ) )
        {
            logger.debug( "NOT a POM: {}", path );
            return null;
        }

        final StoreKey key = LocationUtils.getKey( item );

        final URI source = AproxDepgraphUtils.toDiscoveryURI( key );

        final List<? extends Location> locations = discoveryConfig.getLocations();
        final Set<String> enabledPatchers = discoveryConfig.getEnabledPatchers();
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

            final Map<String, String> metadata = metadataScannerSupport.scan( result.getSelectedRef(), locations, pomView, item );
            result.setMetadata( metadata );

            if ( storeRelationships )
            {
                logger.info( "Storing discovered relationships." );
                final Set<ProjectRelationship<?>> rejected = dataManager.storeRelationships( result.getAcceptedRelationships() );
                dataManager.addMetadata( result.getSelectedRef(), metadata );

                result = new DiscoveryResult( result.getSource(), result, rejected );
            }
            else
            {
                logger.info( "NOT storing discovered relationships." );
            }
        }

        return result;
    }

}
