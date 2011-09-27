package org.commonjava.web.maven.proxy.rest.util.group;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;
import org.commonjava.web.maven.proxy.rest.util.Downloader;

public class MavenMetadataHandler
    implements GroupPathHandler
{

    private static final String METADATA_NAME = "maven-metadata.xml";

    private final Logger logger = new Logger( getClass() );

    @Override
    public boolean canHandle( final String path )
    {
        return path.endsWith( METADATA_NAME );
    }

    @Override
    public File handle( final Group group, final List<Repository> repos, final String path,
                        final Downloader downloader, final ProxyConfiguration config )
    {
        File dir = new File( config.getRepositoryRootDirectory(), group.getName() );
        File target = new File( dir, path );

        if ( target.exists() )
        {
            return target;
        }
        else
        {
            Set<File> files = downloader.downloadAll( repos, path );

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

                return target;
            }
        }

        return null;
    }

}
