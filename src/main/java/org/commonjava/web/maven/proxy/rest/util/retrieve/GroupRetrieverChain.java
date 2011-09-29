package org.commonjava.web.maven.proxy.rest.util.retrieve;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.web.maven.proxy.model.ArtifactStore;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.rest.util.FileManager;

@Singleton
public class GroupRetrieverChain
{

    @Inject
    private Retrievers retrievers;

    @Inject
    private FileManager downloader;

    public File retrieve( final Group group, final List<ArtifactStore> stores, final String path )
    {
        for ( GroupPathRetriever handler : retrievers.getRetrievers() )
        {
            if ( handler.canHandle( path ) )
            {
                return handler.handle( group, stores, path );
            }
        }

        return downloader.downloadFirst( stores, path );
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
