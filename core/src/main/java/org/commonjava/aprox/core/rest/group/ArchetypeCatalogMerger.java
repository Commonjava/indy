/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.rest.group;

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
