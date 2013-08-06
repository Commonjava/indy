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
import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class ArchetypeCatalogMerger
    implements MetadataMerger
{

    public static final String CATALOG_NAME = "archetype-catalog.xml";

    public static final String CATALOG_MERGEINFO_SUFFIX = ".info";

    private final Logger logger = new Logger( getClass() );

    @Override
    public byte[] merge( final Set<Transfer> sources, final Group group, final String path )
    {
        final ArchetypeCatalog master = new ArchetypeCatalog();
        final ArchetypeCatalogXpp3Reader reader = new ArchetypeCatalogXpp3Reader();
        final FileReader fr = null;

        boolean merged = false;

        final Set<String> seen = new HashSet<String>();
        for ( final Transfer src : sources )
        {
            try
            {
                final ArchetypeCatalog catalog = reader.read( src.openInputStream(), false );

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
                logger.error( "Cannot read archetype catalog: %s from artifact-store: %s. Reason: %s", e,
                              src.getPath(), key, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                final StoreKey key = getKey( src );
                logger.error( "Cannot parse archetype catalog: %s from artifact-store: %s. Reason: %s", e,
                              src.getPath(), key, e.getMessage() );
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
                logger.error( "Cannot write consolidated archetype catalog: %s to: %s. Reason: %s", e, path,
                              group.getKey(), e.getMessage() );
            }
        }

        return null;
    }

}
