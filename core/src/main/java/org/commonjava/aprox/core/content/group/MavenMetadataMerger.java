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
package org.commonjava.aprox.core.content.group;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class MavenMetadataMerger
    implements MetadataMerger
{

    public class SnapshotVersionComparator
        implements Comparator<SnapshotVersion>
    {
        @Override
        public int compare( final SnapshotVersion first, final SnapshotVersion second )
        {
            int comp = first.getVersion()
                            .compareTo( second.getVersion() );
            if ( comp == 0 )
            {
                comp = first.getExtension()
                            .compareTo( second.getExtension() );
            }

            return comp;
        }
    }

    public static final String METADATA_NAME = "maven-metadata.xml";

    public static final String METADATA_SHA_NAME = METADATA_NAME + ".sha1";

    public static final String METADATA_MD5_NAME = METADATA_NAME + ".md5";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public byte[] merge( final Collection<Transfer> sources, final Group group, final String path )
    {
        final Metadata master = new Metadata();
        final MetadataXpp3Reader reader = new MetadataXpp3Reader();
        final FileReader fr = null;
        InputStream stream = null;

        boolean merged = false;
        for ( final Transfer src : sources )
        {
            try
            {
                stream = src.openInputStream();

                // there is a lot of junk in here to make up for Metadata's anemic merge() method.
                final Metadata md = reader.read( stream, false );

                if ( md.getGroupId() != null )
                {
                    master.setGroupId( md.getGroupId() );
                }

                if ( md.getArtifactId() != null )
                {
                    master.setArtifactId( md.getArtifactId() );
                }

                if ( md.getVersion() != null )
                {
                    master.setVersion( md.getVersion() );
                }

                master.merge( md );

                if ( master.getVersioning() == null )
                {
                    master.setVersioning( new Versioning() );
                }

                if ( md.getVersioning() != null )
                {
                    final List<SnapshotVersion> snapshotVersions = master.getVersioning()
                                                                         .getSnapshotVersions();
                    boolean added = false;
                    for ( final SnapshotVersion snap : md.getVersioning()
                                                         .getSnapshotVersions() )
                    {
                        if ( !snapshotVersions.contains( snap ) )
                        {
                            snapshotVersions.add( snap );
                            added = true;
                        }
                    }

                    if ( added )
                    {
                        Collections.sort( snapshotVersions, new SnapshotVersionComparator() );
                    }
                }

                merged = true;
            }
            catch ( final IOException e )
            {
                final StoreKey key = getKey( src );
                logger.error( String.format( "Cannot read metadata: %s from artifact-store: %s. Reason: %s", src.getPath(), key, e.getMessage() ), e );
            }
            catch ( final XmlPullParserException e )
            {
                final StoreKey key = getKey( src );
                logger.error( String.format( "Cannot parse metadata: %s from artifact-store: %s. Reason: %s", src.getPath(), key, e.getMessage() ), e );
            }
            finally
            {
                closeQuietly( fr );
                closeQuietly( stream );
            }
        }

        if ( merged )
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try
            {
                new MetadataXpp3Writer().write( baos, master );

                return baos.toByteArray();
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Cannot write consolidated metadata: %s to: %s. Reason: %s", path, group.getKey(), e.getMessage() ), e );
            }
        }

        return null;
    }

}
