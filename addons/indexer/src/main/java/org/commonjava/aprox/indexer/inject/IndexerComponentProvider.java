/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.indexer.inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.maven.index.Indexer;
import org.apache.maven.index.IndexerEngine;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.apache.maven.index.updater.IndexUpdater;
import org.commonjava.aprox.subsys.maven.MavenComponentException;
import org.commonjava.aprox.subsys.maven.MavenComponentManager;

@ApplicationScoped
public class IndexerComponentProvider
{

    @Inject
    private MavenComponentManager compManager;

    private Indexer indexer;

    private IndexerEngine indexerEngine;

    private Scanner scanner;

    private IndexUpdater indexUpdater;

    private IndexCreatorSet indexCreators;

    @Produces
    @Default
    public Scanner getScanner()
        throws MavenComponentException
    {
        if ( scanner == null )
        {
            scanner = compManager.getComponent( Scanner.class );
        }

        return scanner;
    }

    @Produces
    @Default
    public Indexer getIndexer()
        throws MavenComponentException
    {
        if ( indexer == null )
        {
            indexer = compManager.getComponent( Indexer.class );
        }

        return indexer;
    }

    @Produces
    @Default
    public IndexerEngine getIndexerEngine()
        throws MavenComponentException
    {
        if ( indexerEngine == null )
        {
            indexerEngine = compManager.getComponent( IndexerEngine.class );
        }

        return indexerEngine;
    }

    @Produces
    @Default
    public IndexUpdater getIndexUpdater()
        throws MavenComponentException
    {
        if ( indexUpdater == null )
        {
            indexUpdater = compManager.getComponent( IndexUpdater.class );
        }

        return indexUpdater;
    }

    @Produces
    @Default
    public IndexCreatorSet getIndexCreators()
        throws MavenComponentException
    {
        if ( indexCreators == null )
        {
            List<IndexCreator> creators = new ArrayList<IndexCreator>( 4 );
            creators.add( compManager.getComponent( IndexCreator.class, MinimalArtifactInfoIndexCreator.ID ) );
            creators.add( compManager.getComponent( IndexCreator.class, JarFileContentsIndexCreator.ID ) );
            creators.add( compManager.getComponent( IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID ) );
            creators.add( compManager.getComponent( IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID ) );

            creators = Collections.unmodifiableList( creators );

            indexCreators = new IndexCreatorSet( creators );
        }

        return indexCreators;
    }

}
