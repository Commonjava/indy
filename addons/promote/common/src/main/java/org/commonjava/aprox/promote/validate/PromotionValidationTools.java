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
package org.commonjava.aprox.promote.validate;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentDigest;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.galley.AproxLocationResolver;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.promote.model.PromoteRequest;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.graph.MavenModelProcessor;
import org.commonjava.cartographer.graph.discover.DiscoveryConfig;
import org.commonjava.cartographer.graph.discover.DiscoveryResult;
import org.commonjava.cartographer.graph.discover.meta.MetadataScannerSupport;
import org.commonjava.cartographer.graph.discover.patch.DepgraphPatcherConstants;
import org.commonjava.cartographer.graph.discover.patch.PatcherSupport;
import org.commonjava.cartographer.spi.graph.discover.DiscoverySourceManager;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.meta.MavenMetadataView;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.maven.util.ArtifactPathUtils;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.aprox.promote.validate.util.ReadOnlyTransfer.readOnlyWrapper;
import static org.commonjava.aprox.promote.validate.util.ReadOnlyTransfer.readOnlyWrappers;

/**
 * Created by jdcasey on 9/11/15.
 */
public class PromotionValidationTools
{
    @Inject
    private ContentManager contentManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private MavenPomReader pomReader;

    @Inject
    private MavenMetadataReader metadataReader;

    @Inject
    private PatcherSupport patcherSupport;

    @Inject
    private MavenModelProcessor modelProcessor;

    @Inject
    private DiscoverySourceManager sourceManager;

    @Inject
    private TypeMapper typeMapper;

    @Inject
    private TransferManager transferManager;

    protected PromotionValidationTools()
    {
    }

    public PromotionValidationTools( ContentManager manager, StoreDataManager storeDataManager,
                                     MavenPomReader pomReader, MavenMetadataReader metadataReader,
                                     PatcherSupport patcherSupport, MavenModelProcessor modelProcessor,
                                     DiscoverySourceManager sourceManager, TypeMapper typeMapper,
                                     TransferManager transferManager )
    {
        contentManager = manager;
        this.storeDataManager = storeDataManager;
        this.pomReader = pomReader;
        this.metadataReader = metadataReader;
        this.patcherSupport = patcherSupport;
        this.modelProcessor = modelProcessor;
        this.sourceManager = sourceManager;
        this.typeMapper = typeMapper;
        this.transferManager = transferManager;
    }

    public String toArtifactPath( ProjectVersionRef ref )
            throws TransferException
    {
        return ArtifactPathUtils.formatArtifactPath( ref, typeMapper );
    }

    public String toMetadataPath( ProjectRef ref, String filename )
            throws TransferException
    {
        return ArtifactPathUtils.formatMetadataPath( ref, filename );
    }

    public String toMetadataPath( String groupId, String filename )
            throws TransferException
    {
        return ArtifactPathUtils.formatMetadataPath( groupId, filename );
    }

    public Set<ProjectRelationship<?, ?>> getRelationshipsForPom( String path, DiscoveryConfig dc, PromoteRequest request,
                                                                   StoreKey... extraLocations )
            throws AproxWorkflowException, GalleyMavenException, CartoDataException, AproxDataException
    {
        ArtifactRef artifactRef = getArtifact( path );
        if ( artifactRef == null )
        {
            return null;
        }

        StoreKey key = request.getSource();
        Transfer transfer = getTransfer( key, path );

        List<Location> locations = new ArrayList<>( extraLocations.length + 1 );
        locations.add( transfer.getLocation() );
        addLocations( locations, extraLocations );

        MavenPomView pomView =
                pomReader.read( artifactRef.asProjectVersionRef(), transfer, locations, MavenPomView.ALL_PROFILES );

        URI source = sourceManager.createSourceURI( key.toString() );

        DiscoveryResult discoveryResult = modelProcessor.readRelationships( pomView, source, dc );
        discoveryResult =
                patcherSupport.patch( discoveryResult, DepgraphPatcherConstants.ALL_PATCHERS, locations, pomView,
                                      transfer );

        return discoveryResult.getAcceptedRelationships();
    }

    public void addLocations( List<Location> locations, StoreKey... extraLocations )
            throws AproxDataException
    {
        for ( StoreKey extra : extraLocations )
        {
            ArtifactStore store = getArtifactStore( extra );
            locations.add( LocationUtils.toLocation( store ) );
        }
    }

    public MavenPomView readPom( String path, PromoteRequest request, StoreKey... extraLocations )
            throws AproxWorkflowException, GalleyMavenException, AproxDataException
    {
        ArtifactRef artifactRef = getArtifact( path );
        if ( artifactRef == null )
        {
            return null;
        }

        StoreKey key = request.getSource();
        Transfer transfer = getTransfer( key, path );

        List<Location> locations = new ArrayList<>( extraLocations.length + 1 );
        locations.add( transfer.getLocation() );
        addLocations( locations, extraLocations );

        return pomReader.read( artifactRef.asProjectVersionRef(), transfer, locations, MavenPomView.ALL_PROFILES );
    }

    public MavenPomView readLocalPom( String path, PromoteRequest request )
            throws AproxWorkflowException, GalleyMavenException
    {
        ArtifactRef artifactRef = getArtifact( path );
        if ( artifactRef == null )
        {
            throw new AproxWorkflowException( "Invalid artifact path: %s. Could not parse ArtifactRef from path.", path );
        }

        StoreKey key = request.getSource();
        Transfer transfer = getTransfer( key, path );

        return pomReader.readLocalPom( artifactRef.asProjectVersionRef(), transfer, MavenPomView.ALL_PROFILES );
    }

    public ArtifactRef getArtifact( String path )
    {
        ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        return pathInfo == null ? null : pathInfo.getArtifact();
    }

    public MavenMetadataView getMetadata( ProjectRef ref, List<? extends Location> locations )
            throws GalleyMavenException
    {
        return metadataReader.getMetadata( ref, locations );
    }

    public MavenMetadataView readMetadata( ProjectRef ref, List<Transfer> transfers )
            throws GalleyMavenException
    {
        return metadataReader.readMetadata( ref, transfers );
    }

    public MavenMetadataView getMetadata( ProjectRef ref, List<? extends Location> locations,
                                          EventMetadata eventMetadata )
            throws GalleyMavenException
    {
        return metadataReader.getMetadata( ref, locations, eventMetadata );
    }

    public MavenMetadataView readMetadata( ProjectRef ref, List<Transfer> transfers, EventMetadata eventMetadata )
            throws GalleyMavenException
    {
        return metadataReader.readMetadata( ref, transfers, eventMetadata );
    }

    public MavenPomView read( ProjectVersionRef ref, Transfer pom, List<? extends Location> locations,
                              String... activeProfileLocations )
            throws GalleyMavenException
    {
        return pomReader.read( ref, pom, locations, activeProfileLocations );
    }

    public MavenPomView read( ProjectVersionRef ref, List<? extends Location> locations, boolean cache,
                              EventMetadata eventMetadata, String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, cache, eventMetadata, activeProfileIds );
    }

    public MavenPomView read( ProjectVersionRef ref, List<? extends Location> locations, boolean cache,
                              String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, cache, activeProfileIds );
    }

    public MavenPomView readLocalPom( ProjectVersionRef ref, Transfer transfer, String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, activeProfileIds );
    }

    public MavenPomView read( ProjectVersionRef ref, List<? extends Location> locations, String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, activeProfileIds );
    }

    public MavenPomView readLocalPom( ProjectVersionRef ref, Transfer transfer, boolean cache,
                                      EventMetadata eventMetadata, String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, cache, eventMetadata, activeProfileIds );
    }

    public MavenPomView readLocalPom( ProjectVersionRef ref, Transfer transfer, EventMetadata eventMetadata,
                                      String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, eventMetadata, activeProfileIds );
    }

    public MavenPomView read( ProjectVersionRef ref, List<? extends Location> locations, EventMetadata eventMetadata,
                              String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.read( ref, locations, eventMetadata, activeProfileIds );
    }

    public MavenPomView readLocalPom( ProjectVersionRef ref, Transfer transfer, boolean cache,
                                      String... activeProfileIds )
            throws GalleyMavenException
    {
        return pomReader.readLocalPom( ref, transfer, cache, activeProfileIds );
    }

    public MavenPomView read( ProjectVersionRef ref, Transfer pom, List<? extends Location> locations,
                              EventMetadata eventMetadata, String... activeProfileLocations )
            throws GalleyMavenException
    {
        return pomReader.read( ref, pom, locations, eventMetadata, activeProfileLocations );
    }

    public Transfer getTransfer( List<ArtifactStore> stores, String path )
            throws AproxWorkflowException
    {
        return readOnlyWrapper( contentManager.getTransfer( stores, path, TransferOperation.DOWNLOAD ) );
    }

    public Transfer getTransfer( StoreKey storeKey, String path )
            throws AproxWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Retrieving transfer for: {} in {}", path, storeKey );
        Transfer result = readOnlyWrapper( contentManager.getTransfer( storeKey, path, TransferOperation.DOWNLOAD ) );
        logger.info( "Result: {}", result );
        return result;
    }

    public Transfer getTransfer( ArtifactStore store, String path )
            throws AproxWorkflowException
    {
        return readOnlyWrapper( contentManager.getTransfer( store, path, TransferOperation.DOWNLOAD ) );
    }

    public Transfer retrieve( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return readOnlyWrapper( contentManager.retrieve( store, path, eventMetadata ) );
    }

    public Transfer retrieve( ArtifactStore store, String path )
            throws AproxWorkflowException
    {
        return readOnlyWrapper( contentManager.retrieve( store, path ) );
    }

    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return readOnlyWrappers( contentManager.retrieveAll( stores, path, eventMetadata ) );
    }

    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path )
            throws AproxWorkflowException
    {
        return readOnlyWrappers( contentManager.retrieveAll( stores, path ) );
    }

    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return readOnlyWrapper( contentManager.retrieveFirst( stores, path, eventMetadata ) );
    }

    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path )
            throws AproxWorkflowException
    {
        return readOnlyWrapper( contentManager.retrieveFirst( stores, path ) );
    }

    public List<StoreResource> list( ArtifactStore store, String path )
            throws AproxWorkflowException
    {
        return contentManager.list( store, path );
    }

    public List<StoreResource> list( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws AproxWorkflowException
    {
        return contentManager.list( store, path, eventMetadata );
    }

    public List<StoreResource> list( List<? extends ArtifactStore> stores, String path )
            throws AproxWorkflowException
    {
        return contentManager.list( stores, path );
    }

    public Map<ContentDigest, String> digest( StoreKey key, String path, ContentDigest... types )
            throws AproxWorkflowException
    {
        return contentManager.digest( key, path, types );
    }

    public HttpExchangeMetadata getHttpMetadata( Transfer txfr )
            throws AproxWorkflowException
    {
        return contentManager.getHttpMetadata( txfr );
    }

    public HttpExchangeMetadata getHttpMetadata( StoreKey storeKey, String path )
            throws AproxWorkflowException
    {
        return contentManager.getHttpMetadata( storeKey, path );
    }

    public HostedRepository getHostedRepository( String name )
            throws AproxDataException
    {
        return storeDataManager.getHostedRepository( name );
    }

    public RemoteRepository getRemoteRepository( String name )
            throws AproxDataException
    {
        return storeDataManager.getRemoteRepository( name );
    }

    public Group getGroup( String name )
            throws AproxDataException
    {
        return storeDataManager.getGroup( name );
    }

    public ArtifactStore getArtifactStore( StoreKey key )
            throws AproxDataException
    {
        return storeDataManager.getArtifactStore( key );
    }

    public List<ArtifactStore> getAllArtifactStores()
            throws AproxDataException
    {
        return storeDataManager.getAllArtifactStores();
    }

    public List<? extends ArtifactStore> getAllArtifactStores( StoreType type )
            throws AproxDataException
    {
        return storeDataManager.getAllArtifactStores( type );
    }

    public List<Group> getAllGroups()
            throws AproxDataException
    {
        return storeDataManager.getAllGroups();
    }

    public List<RemoteRepository> getAllRemoteRepositories()
            throws AproxDataException
    {
        return storeDataManager.getAllRemoteRepositories();
    }

    public List<HostedRepository> getAllHostedRepositories()
            throws AproxDataException
    {
        return storeDataManager.getAllHostedRepositories();
    }

    public List<ArtifactStore> getAllConcreteArtifactStores()
            throws AproxDataException
    {
        return storeDataManager.getAllConcreteArtifactStores();
    }

    public List<ArtifactStore> getOrderedConcreteStoresInGroup( String groupName )
            throws AproxDataException
    {
        return storeDataManager.getOrderedConcreteStoresInGroup( groupName );
    }

    public List<ArtifactStore> getOrderedStoresInGroup( String groupName )
            throws AproxDataException
    {
        return storeDataManager.getOrderedStoresInGroup( groupName );
    }

    public Set<Group> getGroupsContaining( StoreKey repo )
            throws AproxDataException
    {
        return storeDataManager.getGroupsContaining( repo );
    }

    public boolean hasRemoteRepository( String name )
    {
        return storeDataManager.hasRemoteRepository( name );
    }

    public boolean hasGroup( String name )
    {
        return storeDataManager.hasGroup( name );
    }

    public boolean hasHostedRepository( String name )
    {
        return storeDataManager.hasHostedRepository( name );
    }

    public boolean hasArtifactStore( StoreKey key )
    {
        return storeDataManager.hasArtifactStore( key );
    }

    public RemoteRepository findRemoteRepository( String url )
    {
        return storeDataManager.findRemoteRepository( url );
    }

    public Transfer getTransfer( StoreResource resource )
    {
        return transferManager.getCacheReference( resource );
    }
}
