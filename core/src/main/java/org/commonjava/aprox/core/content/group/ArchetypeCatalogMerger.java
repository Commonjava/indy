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
package org.commonjava.aprox.core.content.group;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class ArchetypeCatalogMerger
    implements MetadataMerger
{

    public static final String CATALOG_NAME = "archetype-catalog.xml";

    public static final String CATALOG_MERGEINFO_SUFFIX = ".info";

    public static final String CATALOG_SHA_NAME = CATALOG_NAME + ".sha1";

    public static final String CATALOG_MD5_NAME = CATALOG_NAME + ".md5";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public byte[] merge( final Collection<Transfer> sources, final Group group, final String path )
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
