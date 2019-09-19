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
package org.commonjava.indy.pkg.maven.content.group;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.indy.util.LocationUtils.getKey;

@javax.enterprise.context.ApplicationScoped
public class ArchetypeCatalogMerger
{

    public static final String CATALOG_NAME = "archetype-catalog.xml";

    public static final String CATALOG_MERGEINFO_SUFFIX = ".info";

    public static final String CATALOG_SHA_NAME = CATALOG_NAME + ".sha1";

    public static final String CATALOG_MD5_NAME = CATALOG_NAME + ".md5";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public byte[] merge( final Collection<Transfer> sources, final Group group, final String path )
    {
        final ArchetypeCatalog master = new ArchetypeCatalog();
        final ArchetypeCatalogXpp3Reader reader = new ArchetypeCatalogXpp3Reader();
        final FileReader fr = null;
        boolean merged = false;

        final Set<String> seen = new HashSet<String>();
        for ( final Transfer src : sources )
        {
            try(InputStream stream = src.openInputStream())
            {

                final ArchetypeCatalog catalog = reader.read( stream, false );

                for ( final Archetype arch : catalog.getArchetypes() )
                {
                    final String key = arch.getGroupId() + ":" + arch.getArtifactId() + ":" + arch.getVersion();
                    if ( seen.add( key ) )
                    {
                        master.addArchetype( arch );
                    }
                }

                merged = true;
            }
            catch ( final IOException e )
            {
                final StoreKey key = getKey( src );
                logger.error( String.format( "Cannot read archetype catalog: %s from artifact-store: %s. Reason: %s", src.getPath(), key, e.getMessage() ), e );
            }
            catch ( final XmlPullParserException e )
            {
                final StoreKey key = getKey( src );
                logger.error( String.format( "Cannot parse archetype catalog: %s from artifact-store: %s. Reason: %s", src.getPath(), key, e.getMessage() ), e );
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
                new ArchetypeCatalogXpp3Writer().write( baos, master );

                return baos.toByteArray();
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Cannot write consolidated archetype catalog: %s to: %s. Reason: %s", path, group.getKey(), e.getMessage() ), e );
            }
        }

        return null;
    }
}
