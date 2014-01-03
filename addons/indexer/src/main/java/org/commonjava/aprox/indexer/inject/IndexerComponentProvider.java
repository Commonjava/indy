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
