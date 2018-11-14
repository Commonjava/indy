/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.relate.util;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.relate.conf.RelateConfig;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.atlas.maven.graph.model.EProjectDirectRelationships;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.atlas.maven.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.rel.MavenModelProcessor;
import org.commonjava.maven.galley.maven.rel.ModelProcessorConfig;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.maven.galley.maven.model.view.MavenPomView.ALL_PROFILES;

/**
 * Created by ruhan on 3/16/17.
 */
@ApplicationScoped
public class RelateGenerationManager
{
    public static final String POM_SUFFIX = ".pom";

    public static final String REL_SUFFIX = ".rel";

    public static final String REL_DIRECT_GENERATING = "rel-direct-generating";

    @Inject
    private MavenModelProcessor mavenModelProcessor;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private MavenPomReader mavenPomReader;

    @Inject
    private IndyObjectMapper indyObjectMapper;

    @Inject
    private RelateConfig config;

    /**
     * Generate relationship file for pom transfer.
     * @param transfer
     * @param op
     * @return transfer pointing to the generated rel file.
     */
    public Transfer generateRelationshipFile( Transfer transfer, TransferOperation op )
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );
        if ( !config.isEnabled() )
        {
            logger.debug( "Relate Add-on is not enabled." );
            return null;
        }

        logger.debug( "Relate generation for {}", transfer );

        if ( transfer == null )
        {
            logger.debug( "No transfer. No .rel generation performed." );
            return null;
        }

        String txfrPath = transfer.getPath();
        if ( !txfrPath.endsWith( ".pom" ) )
        {
            logger.debug( "This is not a pom transfer." );
            return null;
        }

        ArtifactPathInfo artPathInfo = ArtifactPathInfo.parse( txfrPath );
        if ( artPathInfo == null )
        {
            logger.debug( "Not an artifact download ({}). No .rel generation performed.", txfrPath );
            return null;
        }

        ConcreteResource pomResource = transfer.getResource();

        StoreKey storeKey = StoreKey.fromString( transfer.getLocation().getName() );
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( storeKey );
        }
        catch ( final IndyDataException ex )
        {
            logger.error( "Error retrieving artifactStore with key " + storeKey, ex );
            return null;
        }

        logger.debug( "Generate .rel corresponding to associated POM download: {}/{}", storeKey,
                      pomResource.getPath() );

        try
        {
            URI source = new URI( pomResource.getLocation().getUri() + REL_SUFFIX );

            ProjectVersionRef ref = artPathInfo.getProjectId();

            // get all groups that this store is a member of
            Set<ArtifactStore> stores = new HashSet<>();
            stores.add( store );
            stores.addAll( storeManager.query().getGroupsContaining( store.getKey() ) );

            List<? extends Location> supplementalLocations =
                            LocationUtils.toLocations( stores.toArray( new ArtifactStore[0] ) );

            MavenPomView pomView = mavenPomReader.read( ref, transfer, supplementalLocations, ALL_PROFILES );

            EProjectDirectRelationships rel =
                            mavenModelProcessor.readRelationships( pomView, source, new ModelProcessorConfig() );

            Transfer transferRel = transfer.getSiblingMeta( REL_SUFFIX );
            writeRelationships( rel, transferRel, op );

            return transferRel;
        }
        catch ( Exception e )
        {
            logger.error( "Error generating .rel file for " + txfrPath + " from store " + store, e );
            return null;
        }
    }

    private void writeRelationships( final EProjectDirectRelationships rels, Transfer transfer,
                                            TransferOperation op ) throws IOException
    {
        try (OutputStream out = transfer.openOutputStream( op, true ))
        {
            String json = indyObjectMapper.writeValueAsString( rels );
            out.write( json.getBytes() );
        }
    }
}
