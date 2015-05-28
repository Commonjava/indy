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
package org.commonjava.aprox.core.content;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.core.content.group.GroupMergeHelper;
import org.commonjava.aprox.core.content.group.MavenMetadataMerger;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.util.SnapshotUtils;
import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.part.SnapshotPart;
import org.commonjava.maven.galley.maven.parse.GalleyMavenXMLException;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.model.TypeMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@ApplicationScoped
public class MavenMetadataGenerator
    extends AbstractMergedContentGenerator
{

    private static final String ARTIFACT_ID = "artifactId";

    private static final String GROUP_ID = "groupId";

    private static final String VERSION = "version";

    private static final String LAST_UPDATED = "lastUpdated";

    private static final String TIMESTAMP = "timestamp";

    private static final String BUILD_NUMBER = "buildNumber";

    private static final String EXTENSION = "extension";

    private static final String VALUE = "value";

    private static final String UPDATED = "updated";

    private static final String LOCAL_COPY = "localCopy";

    private static final String LATEST = "latest";

    private static final String RELEASE = "release";

    private static final String CLASSIFIER = "classifier";

    private static final Set<String> HANDLED_FILENAMES = Collections.unmodifiableSet( new HashSet<String>()
    {

        {
            add( MavenMetadataMerger.METADATA_NAME );
            add( MavenMetadataMerger.METADATA_MD5_NAME );
            add( MavenMetadataMerger.METADATA_SHA_NAME );
        }

        private static final long serialVersionUID = 1L;

    } );

    @Inject
    private XMLInfrastructure xml;

    @Inject
    private TypeMapper typeMapper;

    @Inject
    private MavenMetadataMerger merger;

    protected MavenMetadataGenerator()
    {
    }

    public MavenMetadataGenerator( final DownloadManager fileManager, final StoreDataManager storeManager,
                                   final XMLInfrastructure xml, final TypeMapper typeMapper,
                                   final MavenMetadataMerger merger, final GroupMergeHelper mergeHelper )
    {
        super( fileManager, storeManager, mergeHelper );
        this.xml = xml;
        this.typeMapper = typeMapper;
        this.merger = merger;
    }

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        // metadata merging is something else...don't handle it here.
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            return null;
        }

        if ( !canProcess( path ) )
        {
            return null;
        }

        boolean generated = false;

        // TODO: Generation of plugin metadata files (groupId-level) is harder, and requires cracking open the jar file
        // This is because that's the only place the plugin prefix can be reliably retrieved from.

        // regardless, we will need this first level of listings. What we do with it will depend on the logic below...
        final String parentPath = Paths.get( path )
                                       .getParent()
                                       .toString();

        final List<StoreResource> firstLevel = fileManager.list( store, parentPath );

        String toGenPath = path;
        if ( !path.endsWith( MavenMetadataMerger.METADATA_NAME ) )
        {
            toGenPath = normalize( normalize( parentPath( toGenPath ) ), MavenMetadataMerger.METADATA_NAME );
        }

        ArtifactPathInfo snapshotPomInfo = null;

        if ( parentPath.endsWith( SnapshotUtils.LOCAL_SNAPSHOT_VERSION_PART ) )
        {
            // If we're in a version directory, first-level listing should include a .pom file
            for ( final StoreResource resource : firstLevel )
            {
                if ( resource.getPath()
                             .endsWith( ".pom" ) )
                {
                    snapshotPomInfo = ArtifactPathInfo.parse( resource.getPath() );
                    break;
                }
            }
        }

        if ( snapshotPomInfo != null )
        {
            generated = writeSnapshotMetadata( snapshotPomInfo, firstLevel, store, toGenPath );
        }
        else
        {
            generated = writeVersionMetadata( firstLevel, store, toGenPath );
        }

        return generated ? fileManager.getStorageReference( store, path ) : null;
    }

    @Override
    public List<StoreResource> generateDirectoryContent( final ArtifactStore store, final String path,
                                                         final List<StoreResource> existing )
        throws AproxWorkflowException
    {
        final StoreResource mdResource =
            new StoreResource( LocationUtils.toLocation( store ), Paths.get( path, MavenMetadataMerger.METADATA_NAME )
                                                                       .toString() );

        if ( existing.contains( mdResource ) )
        {
            return null;
        }

        // regardless, we will need this first level of listings. What we do with it will depend on the logic below...
        final List<StoreResource> firstLevelFiles = fileManager.list( store, path );

        ArtifactPathInfo samplePomInfo = null;
        nextTopResource: for ( final StoreResource topResource : firstLevelFiles )
        {
            final String topPath = topResource.getPath();
            if ( topPath.endsWith( ".pom" ) )
            {
                samplePomInfo = ArtifactPathInfo.parse( topPath );
                break;
            }
            else if ( topPath.endsWith( "/" ) )
            {
                final List<StoreResource> secondLevelListing = fileManager.list( store, topPath );
                for ( final StoreResource fileResource : secondLevelListing )
                {
                    if ( fileResource.getPath()
                                     .endsWith( ".pom" ) )
                    {
                        if ( samplePomInfo == null )
                        {
                            samplePomInfo = ArtifactPathInfo.parse( fileResource.getPath() );
                            break nextTopResource;
                        }

                        continue nextTopResource;
                    }
                }
            }
        }

        // TODO: Generation of plugin metadata files (groupId-level) is harder, and requires cracking open the jar file
        // This is because that's the only place the plugin prefix can be reliably retrieved from.
        // We won't worry about this for now.
        if ( samplePomInfo != null )
        {
            final List<StoreResource> result = new ArrayList<StoreResource>();
            result.add( mdResource );
            result.add( new StoreResource( LocationUtils.toLocation( store ),
                                           Paths.get( path, MavenMetadataMerger.METADATA_MD5_NAME )
                                                .toString() ) );
            result.add( new StoreResource( LocationUtils.toLocation( store ),
                                           Paths.get( path, MavenMetadataMerger.METADATA_SHA_NAME )
                                                .toString() ) );
            return result;
        }

        return null;
    }

    @Override
    public Transfer generateGroupFileContent( final Group group, final List<ArtifactStore> members, final String path )
        throws AproxWorkflowException
    {
        if ( !canProcess( path ) )
        {
            return null;
        }

        final Transfer target = fileManager.getStorageReference( group, path );

        if ( !target.exists() )
        {
            String toMergePath = path;
            if ( !path.endsWith( MavenMetadataMerger.METADATA_NAME ) )
            {
                toMergePath = normalize( normalize( parentPath( toMergePath ) ), MavenMetadataMerger.METADATA_NAME );
            }

            final List<Transfer> sources = fileManager.retrieveAll( members, toMergePath );
            final byte[] merged = merger.merge( sources, group, toMergePath );
            if ( merged != null )
            {
                OutputStream fos = null;
                try
                {
                    fos = target.openOutputStream( TransferOperation.GENERATE, true );
                    fos.write( merged );

                }
                catch ( final IOException e )
                {
                    throw new AproxWorkflowException( "Failed to write merged metadata to: {}.\nError: {}", e, target,
                                                      e.getMessage() );
                }
                finally
                {
                    closeQuietly( fos );
                }

                helper.writeMergeInfo( merged, sources, group, toMergePath );
            }
        }

        if ( target.exists() )
        {
            return target;
        }

        return null;
    }

    private boolean canProcess( final String path )
    {
        for ( final String filename : HANDLED_FILENAMES )
        {
            if ( path.endsWith( filename ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<StoreResource> generateGroupDirectoryContent( final Group group, final List<ArtifactStore> members,
                                                              final String path )
        throws AproxWorkflowException
    {
        return generateDirectoryContent( group, path, Collections.<StoreResource> emptyList() );
    }

    @Override
    protected String getMergedMetadataName()
    {
        return MavenMetadataMerger.METADATA_NAME;
    }

    private boolean writeVersionMetadata( final List<StoreResource> firstLevelFiles, final ArtifactStore store,
                                          final String path )
        throws AproxWorkflowException
    {
        ArtifactPathInfo samplePomInfo = null;

        // first level will contain version directories...for each directory, we need to verify the presence of a .pom file before including 
        // as a valid version
        final List<SingleVersion> versions = new ArrayList<SingleVersion>();
        nextTopResource: for ( final StoreResource topResource : firstLevelFiles )
        {
            final String topPath = topResource.getPath();
            if ( topPath.endsWith( "/" ) )
            {
                final List<StoreResource> secondLevelListing = fileManager.list( store, topPath );
                for ( final StoreResource fileResource : secondLevelListing )
                {
                    if ( fileResource.getPath()
                                     .endsWith( ".pom" ) )
                    {
                        versions.add( VersionUtils.createSingleVersion( new File( topPath ).getName() ) );
                        if ( samplePomInfo == null )
                        {
                            samplePomInfo = ArtifactPathInfo.parse( fileResource.getPath() );
                        }

                        continue nextTopResource;
                    }
                }
            }
        }

        if ( versions.isEmpty() )
        {
            return false;
        }

        Collections.sort( versions );

        final Transfer metadataFile = fileManager.getStorageReference( store, path );
        OutputStream stream = null;
        try
        {
            final Document doc = xml.newDocumentBuilder()
                                    .newDocument();

            final Map<String, String> coordMap = new HashMap<String, String>();
            coordMap.put( ARTIFACT_ID, samplePomInfo.getArtifactId() );
            coordMap.put( GROUP_ID, samplePomInfo.getGroupId() );

            final String lastUpdated = SnapshotUtils.generateUpdateTimestamp( SnapshotUtils.getCurrentTimestamp() );

            doc.appendChild( doc.createElementNS( doc.getNamespaceURI(), "metadata" ) );
            xml.createElement( doc.getDocumentElement(), null, coordMap );

            final Map<String, String> versioningMap = new HashMap<String, String>();
            versioningMap.put( LAST_UPDATED, lastUpdated );

            final SingleVersion latest = versions.get( versions.size() - 1 );
            versioningMap.put( LATEST, latest.renderStandard() );

            SingleVersion release = null;
            for ( int i = versions.size() - 1; i >= 0; i-- )
            {
                final SingleVersion r = versions.get( i );
                if ( r.isRelease() )
                {
                    release = r;
                    break;
                }
            }

            if ( release != null )
            {
                versioningMap.put( RELEASE, release.renderStandard() );
            }

            xml.createElement( doc, "versioning", versioningMap );
            final Element versionsElem =
                xml.createElement( doc, "versioning/versions", Collections.<String, String> emptyMap() );

            for ( final SingleVersion version : versions )
            {
                final Element vElem = doc.createElement( VERSION );
                vElem.setTextContent( version.renderStandard() );

                versionsElem.appendChild( vElem );
            }

            final String xmlStr = xml.toXML( doc, true );
            stream = metadataFile.openOutputStream( TransferOperation.GENERATE );
            stream.write( xmlStr.getBytes( "UTF-8" ) );
        }
        catch ( final GalleyMavenXMLException e )
        {
            throw new AproxWorkflowException( "Failed to generate maven metadata file: %s. Reason: %s", e, path,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to write generated maven metadata file: %s. Reason: %s", e,
                                              metadataFile, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }

        return true;
    }

    private boolean writeSnapshotMetadata( final ArtifactPathInfo info, final List<StoreResource> files,
                                           final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        // first level will contain files that have the timestamp-buildnumber version suffix...for each, we need to parse this info.
        final Map<SnapshotPart, Set<ArtifactPathInfo>> infosBySnap = new HashMap<SnapshotPart, Set<ArtifactPathInfo>>();
        for ( final StoreResource resource : files )
        {
            final ArtifactPathInfo resInfo = ArtifactPathInfo.parse( resource.getPath() );
            if ( resInfo != null )
            {
                final SnapshotPart snap = resInfo.getSnapshotInfo();
                Set<ArtifactPathInfo> infos = infosBySnap.get( snap );
                if ( infos == null )
                {
                    infos = new HashSet<ArtifactPathInfo>();
                    infosBySnap.put( snap, infos );
                }

                infos.add( resInfo );
            }
        }

        if ( infosBySnap.isEmpty() )
        {
            return false;
        }

        final List<SnapshotPart> snaps = new ArrayList<SnapshotPart>( infosBySnap.keySet() );
        Collections.sort( snaps );

        final Transfer metadataFile = fileManager.getStorageReference( store, path );
        OutputStream stream = null;
        try
        {
            final Document doc = xml.newDocumentBuilder()
                                    .newDocument();

            final Map<String, String> coordMap = new HashMap<String, String>();
            coordMap.put( ARTIFACT_ID, info.getArtifactId() );
            coordMap.put( GROUP_ID, info.getGroupId() );
            coordMap.put( VERSION, info.getVersion() );

            final String lastUpdated = SnapshotUtils.generateUpdateTimestamp( SnapshotUtils.getCurrentTimestamp() );

            doc.appendChild( doc.createElementNS( doc.getNamespaceURI(), "metadata" ) );
            xml.createElement( doc.getDocumentElement(), null, coordMap );

            xml.createElement( doc, "versioning", Collections.<String, String> singletonMap( LAST_UPDATED, lastUpdated ) );

            SnapshotPart snap = snaps.get( snaps.size() - 1 );
            Map<String, String> snapMap = new HashMap<String, String>();
            if ( snap.isLocalSnapshot() )
            {
                snapMap.put( LOCAL_COPY, Boolean.TRUE.toString() );
            }
            else
            {
                snapMap.put( TIMESTAMP, SnapshotUtils.generateSnapshotTimestamp( snap.getTimestamp() ) );

                snapMap.put( BUILD_NUMBER, Integer.toString( snap.getBuildNumber() ) );
            }

            xml.createElement( doc, "versioning/snapshot", snapMap );

            for ( int i = 0; i < snaps.size(); i++ )
            {
                snap = snaps.get( i );

                // the last one is the most recent.
                final Set<ArtifactPathInfo> infos = infosBySnap.get( snap );
                for ( final ArtifactPathInfo pathInfo : infos )
                {
                    snapMap = new HashMap<String, String>();

                    final TypeAndClassifier tc = new TypeAndClassifier( pathInfo.getType(), pathInfo.getClassifier() );
                    final TypeMapping mapping = typeMapper.lookup( tc );

                    final String classifier = mapping == null ? pathInfo.getClassifier() : mapping.getClassifier();
                    if ( classifier != null && classifier.length() > 0 )
                    {
                        snapMap.put( CLASSIFIER, classifier );
                    }

                    snapMap.put( EXTENSION, mapping == null ? pathInfo.getType() : mapping.getExtension() );
                    snapMap.put( VALUE, pathInfo.getVersion() );
                    snapMap.put( UPDATED, lastUpdated );

                    xml.createElement( doc, "versioning/snapshotVersions/snapshotVersion", snapMap );
                }
            }

            final String xmlStr = xml.toXML( doc, true );
            stream = metadataFile.openOutputStream( TransferOperation.GENERATE );
            stream.write( xmlStr.getBytes( "UTF-8" ) );
        }
        catch ( final GalleyMavenXMLException e )
        {
            throw new AproxWorkflowException( "Failed to generate maven metadata file: %s. Reason: %s", e, path,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to write generated maven metadata file: %s. Reason: %s", e,
                                              metadataFile, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }

        return true;
    }

    // Parking this here, transplanted from ScheduleManager, because this is where it belongs. It might be
    // covered elsewhere, but in case it's not, we can use this as a reference...
    //    public void updateSnapshotVersions( final StoreKey key, final String path )
    //    {
    //        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
    //        if ( pathInfo == null )
    //        {
    //            return;
    //        }
    //
    //        final ArtifactStore store;
    //        try
    //        {
    //            store = dataManager.getArtifactStore( key );
    //        }
    //        catch ( final AproxDataException e )
    //        {
    //            logger.error( String.format( "Failed to update metadata after snapshot deletion. Reason: {}",
    //                                         e.getMessage() ), e );
    //            return;
    //        }
    //
    //        if ( store == null )
    //        {
    //            logger.error( "Failed to update metadata after snapshot deletion in: {}. Reason: Cannot find corresponding ArtifactStore",
    //                          key );
    //            return;
    //        }
    //
    //        final Transfer item = fileManager.getStorageReference( store, path );
    //        if ( item.getParent() == null || item.getParent()
    //                                             .getParent() == null )
    //        {
    //            return;
    //        }
    //
    //        final Transfer metadata = fileManager.getStorageReference( store, item.getParent()
    //                                                                              .getParent()
    //                                                                              .getPath(), "maven-metadata.xml" );
    //
    //        if ( metadata.exists() )
    //        {
    //            //            logger.info( "[UPDATE VERSIONS] Updating snapshot versions for path: {} in store: {}", path, key.getName() );
    //            Reader reader = null;
    //            Writer writer = null;
    //            try
    //            {
    //                reader = new InputStreamReader( metadata.openInputStream() );
    //                final Metadata md = new MetadataXpp3Reader().read( reader );
    //
    //                final Versioning versioning = md.getVersioning();
    //                final List<String> versions = versioning.getVersions();
    //
    //                final String version = pathInfo.getVersion();
    //                String replacement = null;
    //
    //                final int idx = versions.indexOf( version );
    //                if ( idx > -1 )
    //                {
    //                    if ( idx > 0 )
    //                    {
    //                        replacement = versions.get( idx - 1 );
    //                    }
    //
    //                    versions.remove( idx );
    //                }
    //
    //                if ( version.equals( md.getVersion() ) )
    //                {
    //                    md.setVersion( replacement );
    //                }
    //
    //                if ( version.equals( versioning.getLatest() ) )
    //                {
    //                    versioning.setLatest( replacement );
    //                }
    //
    //                final SnapshotPart si = pathInfo.getSnapshotInfo();
    //                if ( si != null )
    //                {
    //                    final SnapshotPart siRepl = SnapshotUtils.extractSnapshotVersionPart( replacement );
    //                    final Snapshot snapshot = versioning.getSnapshot();
    //
    //                    final String siTstamp = SnapshotUtils.generateSnapshotTimestamp( si.getTimestamp() );
    //                    if ( si.isRemoteSnapshot() && siTstamp.equals( snapshot.getTimestamp() )
    //                        && si.getBuildNumber() == snapshot.getBuildNumber() )
    //                    {
    //                        if ( siRepl != null )
    //                        {
    //                            if ( siRepl.isRemoteSnapshot() )
    //                            {
    //                                snapshot.setTimestamp( SnapshotUtils.generateSnapshotTimestamp( siRepl.getTimestamp() ) );
    //                                snapshot.setBuildNumber( siRepl.getBuildNumber() );
    //                            }
    //                            else
    //                            {
    //                                snapshot.setLocalCopy( true );
    //                            }
    //                        }
    //                        else
    //                        {
    //                            versioning.setSnapshot( null );
    //                        }
    //                    }
    //                }
    //
    //                writer = new OutputStreamWriter( metadata.openOutputStream( TransferOperation.GENERATE, true ) );
    //                new MetadataXpp3Writer().write( writer, md );
    //            }
    //            catch ( final IOException e )
    //            {
    //                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}",
    //                              e, item.getFullPath(), metadata, e.getMessage() );
    //            }
    //            catch ( final XmlPullParserException e )
    //            {
    //                logger.error( "Failed to update metadata after snapshot deletion.\n  Snapshot: {}\n  Metadata: {}\n  Reason: {}",
    //                              e, item.getFullPath(), metadata, e.getMessage() );
    //            }
    //            finally
    //            {
    //                closeQuietly( reader );
    //                closeQuietly( writer );
    //            }
    //        }
    //    }
    //
}
