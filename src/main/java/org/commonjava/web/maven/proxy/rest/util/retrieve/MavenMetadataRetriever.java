package org.commonjava.web.maven.proxy.rest.util.retrieve;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.model.ArtifactStore;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.rest.util.FileManager;
import org.commonjava.web.maven.proxy.rest.util.MavenMetadataMerger;

@Singleton
public class MavenMetadataRetriever
    implements GroupPathRetriever
{

    @Inject
    private FileManager downloader;

    @Inject
    private ProxyConfiguration config;

    @Inject
    private MavenMetadataMerger merger;

    @Override
    public boolean canHandle( final String path )
    {
        return path.endsWith( MavenMetadataMerger.METADATA_NAME );
    }

    @Override
    public File handle( final Group group, final List<ArtifactStore> stores, final String path )
    {
        File dir = new File( config.getRepositoryRootDirectory(), group.getName() );
        File target = new File( dir, path );

        if ( target.exists() )
        {
            return target;
        }
        else
        {
            Set<File> files = downloader.downloadAll( stores, path );
            if ( merger.merge( files, target, group, path ) )
            {
                return target;
            }
        }

        return null;
    }

}
