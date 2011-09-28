package org.commonjava.web.maven.proxy.rest.util.retrieve;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;
import org.commonjava.web.maven.proxy.rest.util.Downloader;

@Singleton
public class GroupRetrieverChain
{

    @Inject
    private Retrievers retrievers;

    @Inject
    private Downloader downloader;

    public File retrieve( final Group group, final List<Repository> repos, final String path )
    {
        for ( GroupPathRetriever handler : retrievers.getRetrievers() )
        {
            if ( handler.canHandle( path ) )
            {
                return handler.handle( group, repos, path );
            }
        }

        return downloader.downloadFirst( repos, path );
    }

    @Singleton
    static final class Retrievers
    {
        @Inject
        private MavenMetadataRetriever mavenMetadataRetriever;

        private List<GroupPathRetriever> retrievers;

        public synchronized List<GroupPathRetriever> getRetrievers()
        {
            if ( retrievers == null )
            {
                retrievers = new ArrayList<GroupPathRetriever>();
                retrievers.add( mavenMetadataRetriever );
            }

            return retrievers;
        }
    }

}
