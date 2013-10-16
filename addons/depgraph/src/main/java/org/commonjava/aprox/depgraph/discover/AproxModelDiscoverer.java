/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.post.meta.MetadataScannerSupport;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class AproxModelDiscoverer
{

    private final Logger logger = new Logger( getClass() );

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

    public DiscoveryResult discoverRelationships( final Transfer item, final List<? extends KeyedLocation> locations,
                                                  final Set<String> enabledPatchers, final boolean storeRelationships )
        throws CartoDataException
    {
        final String path = item.getPath();

        if ( !path.endsWith( ".pom" ) )
        {
            logger.info( "NOT a POM: %s", path );
            return null;
        }

        final StoreKey key = LocationUtils.getKey( item );

        final URI source = AproxDepgraphUtils.toDiscoveryURI( key );

        MavenPomView pomView;
        try
        {
            pomView = pomReader.read( item, locations );
        }
        catch ( final GalleyMavenException e )
        {
            throw new CartoDataException( "Failed to parse: %s. Reason: %s", e, item, e.getMessage() );
        }

        DiscoveryResult result = modelProcessor.readRelationships( pomView, source );

        if ( result != null )
        {
            logger.info( "Attempting to patch results for: %s", result.getSelectedRef() );
            result = patchers.patch( result, enabledPatchers, locations, pomView, item );

            final Map<String, String> metadata = metadataScannerSupport.scan( result.getSelectedRef(), locations, pomView, item );
            result.setMetadata( metadata );

            if ( storeRelationships )
            {
                final Set<ProjectRelationship<?>> rejected = dataManager.storeRelationships( result.getAcceptedRelationships() );
                dataManager.addMetadata( result.getSelectedRef(), metadata );

                result = new DiscoveryResult( result.getSource(), result, rejected );
            }
        }

        return result;
    }

}
