package org.commonjava.aprox.core.rest.util;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.util.logging.Logger;

@Singleton
public class MavenMetadataMerger
{

    public static final String METADATA_NAME = "maven-metadata.xml";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private Event<FileStorageEvent> fileEvent;

    public boolean merge( final Set<File> files, final File target, final Group group,
                          final String path )
    {
        Metadata master = new Metadata();
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        FileReader fr = null;

        boolean merged = false;
        for ( File file : files )
        {
            try
            {
                fr = new FileReader( file );
                Metadata md = reader.read( fr );

                master.merge( md );

                merged = true;
            }
            catch ( IOException e )
            {
                logger.error( "Cannot read metadata: %s. Reason: %s", e, file, e.getMessage() );
            }
            catch ( XmlPullParserException e )
            {
                logger.error( "Cannot parse metadata: %s. Reason: %s", e, file, e.getMessage() );
            }
            finally
            {
                closeQuietly( fr );
            }
        }

        if ( merged )
        {
            FileWriter writer = null;
            try
            {
                target.getParentFile().mkdirs();

                writer = new FileWriter( target );
                new MetadataXpp3Writer().write( writer, master );

                if ( fileEvent != null )
                {
                    fileEvent.fire( new FileStorageEvent( FileStorageEvent.Type.GENERATE, group,
                                                          path, target ) );
                }
            }
            catch ( IOException e )
            {
                logger.error( "Cannot write consolidated metadata: %s. Reason: %s", e, target,
                              e.getMessage() );
            }
            finally
            {
                closeQuietly( writer );
            }

            return true;
        }

        return false;
    }

}
