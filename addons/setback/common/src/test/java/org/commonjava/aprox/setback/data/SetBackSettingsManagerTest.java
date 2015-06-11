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
package org.commonjava.aprox.setback.data;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import groovy.text.GStringTemplateEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.core.data.DefaultStoreEventDispatcher;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.commonjava.aprox.subsys.datafile.conf.DataFileConfiguration;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
import org.commonjava.maven.galley.event.EventMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SetBackSettingsManagerTest
{

    private static final String USER_HOME = System.getProperty( "user.home" );

    private StoreDataManager storeManager;

    private SetBackSettingsManager manager;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final ChangeSummary summary = new ChangeSummary( "test-user", "test" );

    @Before
    public void setup()
        throws Exception
    {
        storeManager = new MemoryStoreDataManager( new DefaultStoreEventDispatcher() );

        final File dataSrc = new File( "./src/main/data" );
        final File data = temp.newFolder( "data" );
        FileUtils.copyDirectory( dataSrc, data );

        final DataFileConfiguration config = new DataFileConfiguration( data, temp.newFolder( "work" ) );
        final DataFileManager fileManager = new DataFileManager( config, new DataFileEventManager() );

        final TemplatingEngine templates = new TemplatingEngine( new GStringTemplateEngine(), fileManager );
        manager = new SetBackSettingsManager( storeManager, templates, fileManager );
    }

    @Test
    public void settingsForSingleRemoteRepository_NoGroups()
        throws Exception
    {
        final RemoteRepository remote = new RemoteRepository( "test", "http://foo.bar/" );
        remote.setDescription( "Test Repository" );

        store( remote );

        final StoreKey key = remote.getKey();
        final List<String> lines = generateSettings( key );

        assertThat( "No repository with id: " + remote.getName() + " found in settings.xml",
                    lines.contains( "<id>" + remote.getName() + "</id>" ), equalTo( true ) );
    }

    @Test
    public void settingsForSingleRemoteRepository_GenerateDeleteAndProveNonExistent()
        throws Exception
    {
        final RemoteRepository remote = new RemoteRepository( "test", "http://foo.bar/" );
        remote.setDescription( "Test Repository" );

        store( remote );

        final StoreKey key = remote.getKey();
        final List<String> lines = generateSettings( key );

        assertThat( "No repository with id: " + remote.getName() + " found in settings.xml",
                    lines.contains( "<id>" + remote.getName() + "</id>" ), equalTo( true ) );

        final ArtifactStore store = storeManager.getArtifactStore( key );

        manager.deleteStoreSettings( store );

        assertThat( "Settings.xml for: " + key + " should have been deleted!", manager.getSetBackSettings( key ),
                    nullValue() );
    }

    @Test
    public void settingsForGroup_SingleMemberRemote()
        throws Exception
    {
        final RemoteRepository remote = new RemoteRepository( "test", "http://foo.bar/" );
        remote.setDescription( "Test Repository" );

        store( remote );

        final StoreKey remoteKey = remote.getKey();

        final Group group = new Group( "test-group", remoteKey );
        store( group );

        assertThat( readSettings( group.getKey(), false ), equalTo( null ) );

        final List<String> lines = generateSettings( group.getKey() );

        assertThat( "No repository with id: " + remote.getName() + " found in settings.xml for remote!",
                    lines.contains( "<id>" + remote.getName() + "</id>" ), equalTo( true ) );

    }

    @Test
    public void settingsForGroup_OneRemoteOneHosted_HostedOmittedButAddedInComment()
        throws Exception
    {
        final RemoteRepository remote = new RemoteRepository( "test", "http://foo.bar/" );
        remote.setDescription( "Test Repository" );

        final HostedRepository hosted = new HostedRepository( "test-hosted" );

        store( remote );
        store( hosted );

        final Group group = new Group( "test-group", remote.getKey(), hosted.getKey() );
        store( group );

        System.out.println( "Group members:\n  " + join( group.getConstituents(), "\n  " ) );

        assertThat( readSettings( group.getKey(), false ), equalTo( null ) );

        final List<String> lines = generateSettings( group.getKey() );

        assertThat( "No repository with id: " + remote.getName() + " found in settings.xml for group!",
                    lines.contains( "<id>" + remote.getName() + "</id>" ), equalTo( true ) );

        assertThat( "Repository with id: " + hosted.getName() + " Should not be present in settings.xml for group!",
                    lines.contains( "<id>" + remote.getName() + "</id>" ), equalTo( true ) );

        assertThat( "No entry for remote: " + remote.getKey()
                                                    .toString() + " found in settings.xml constituency comment!",
                    lines.contains( "* " + remote.getKey() ), equalTo( true ) );

        assertThat( "No entry for hosted: " + hosted.getKey()
                                                    .toString() + " found in settings.xml constituency comment!",
                    lines.contains( "* " + hosted.getKey() ), equalTo( true ) );

    }

    @Test
    public void settingsForSingleRemoteRepository_GenerateSpawnsGroupGeneration()
        throws Exception
    {
        final RemoteRepository remote = new RemoteRepository( "test", "http://foo.bar/" );
        remote.setDescription( "Test Repository" );

        store( remote );

        final StoreKey key = remote.getKey();

        final Group group = new Group( "test-group", key );
        store( group );

        assertThat( readSettings( group.getKey(), false ), equalTo( null ) );

        List<String> lines = generateSettings( key );

        assertThat( "No repository with id: " + remote.getName() + " found in settings.xml for remote!",
                    lines.contains( "<id>" + remote.getName() + "</id>" ), equalTo( true ) );

        lines = readSettings( group.getKey(), true );

        assertThat( "No repository with id: " + remote.getName() + " found in settings.xml for group!",
                    lines.contains( "<id>" + remote.getName() + "</id>" ), equalTo( true ) );
    }

    private void store( final ArtifactStore store )
        throws Exception
    {
        storeManager.storeArtifactStore( store, summary, new EventMetadata() );
    }

    private List<String> readSettings( final StoreKey key, final boolean expectExistence )
        throws Exception
    {
        final DataFile settings = manager.getSetBackSettings( key );
        if ( expectExistence )
        {
            assertThat( "Retrieved settings.xml for: " + key + " does not exist!", settings.exists(), equalTo( true ) );

            final List<String> rawLines = settings.readLines();

            System.out.println( join( rawLines, "\n" ) );

            final List<String> lines = new ArrayList<String>();

            for ( final String line : rawLines )
            {
                lines.add( line.trim() );
            }

            return lines;
        }
        else
        {
            assertThat( "Retrieved settings.xml for: " + key + " already exists!", settings, nullValue() );
            return null;
        }
    }

    private List<String> generateSettings( final StoreKey key )
        throws Exception
    {
        final ArtifactStore store = storeManager.getArtifactStore( key );
        final DataFile settings = manager.generateStoreSettings( store );
        assertThat( "settings.xml returned from generateStoreSettings(..) for: " + key + " does not exist!",
                    settings.exists(), equalTo( true ) );

        final List<String> lines = readSettings( key, true );

        final String localRepoLine =
            String.format( "<localRepository>%s</localRepository>",
                           normalize( USER_HOME, ".m2/repository-" + key.getType()
                                                                        .singularEndpointName() + "-" + key.getName() ) );

        assertThat( "Local repository for: " + key + " not configured", lines.contains( localRepoLine ), equalTo( true ) );

        return lines;
    }

}
