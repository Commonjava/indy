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
package org.commonjava.aprox.core.rest.util;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.StoreInputStream;
import org.commonjava.util.logging.Logger;

@Singleton
public class MavenMetadataMerger
{

    public static final String METADATA_NAME = "maven-metadata.xml";

    private final Logger logger = new Logger( getClass() );

    public InputStream merge( final Set<StoreInputStream> sources, final Group group, final String path )
    {
        final Metadata master = new Metadata();
        final MetadataXpp3Reader reader = new MetadataXpp3Reader();
        final FileReader fr = null;

        boolean merged = false;
        for ( final StoreInputStream src : sources )
        {
            try
            {
                final Metadata md = reader.read( src, false );

                master.merge( md );

                merged = true;
            }
            catch ( final IOException e )
            {
                logger.error( "Cannot read metadata: %s from artifact-store: %s. Reason: %s", e, src.getPath(),
                              src.getStoreKey(), e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                logger.error( "Cannot parse metadata: %s from artifact-store: %s. Reason: %s", e, src.getPath(),
                              src.getStoreKey(), e.getMessage() );
            }
            finally
            {
                closeQuietly( fr );
            }
        }

        if ( merged )
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try
            {
                new MetadataXpp3Writer().write( baos, master );

                return new ByteArrayInputStream( baos.toByteArray() );
            }
            catch ( final IOException e )
            {
                logger.error( "Cannot write consolidated metadata: %s to: %s. Reason: %s", e, path, group.getKey(),
                              e.getMessage() );
            }
        }

        return null;
    }

}
