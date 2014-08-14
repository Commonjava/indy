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
import java.util.Set;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class MavenMetadataMerger
    implements MetadataMerger
{

    public static final String METADATA_NAME = "maven-metadata.xml";

    public static final String METADATA_SHA_NAME = METADATA_NAME + ".sha1";

    public static final String METADATA_MD5_NAME = METADATA_NAME + ".md5";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public byte[] merge( final Set<Transfer> sources, final Group group, final String path )
    {
        final Metadata master = new Metadata();
        final MetadataXpp3Reader reader = new MetadataXpp3Reader();
        final FileReader fr = null;

        boolean merged = false;
        for ( final Transfer src : sources )
        {
            try
            {
                final Metadata md = reader.read( src.openInputStream(), false );

                master.merge( md );

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
