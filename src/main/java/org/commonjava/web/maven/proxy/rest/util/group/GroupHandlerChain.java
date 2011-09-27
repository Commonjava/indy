package org.commonjava.web.maven.proxy.rest.util.group;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;
import org.commonjava.web.maven.proxy.rest.util.Downloader;

@Singleton
public class GroupHandlerChain
{

    private final GroupPathHandler[] handlers = { new MavenMetadataHandler() };

    @Inject
    private Downloader downloader;

    @Inject
    private ProxyConfiguration config;

    public File retrieve( final Group group, final List<Repository> repos, final String path )
    {
        for ( GroupPathHandler handler : handlers )
        {
            if ( handler.canHandle( path ) )
            {
                return handler.handle( group, repos, path, downloader, config );
            }
        }

        return downloader.downloadFirst( repos, path );
    }

}
