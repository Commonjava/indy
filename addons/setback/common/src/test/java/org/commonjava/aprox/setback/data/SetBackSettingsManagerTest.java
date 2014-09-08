package org.commonjava.aprox.setback.data;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import groovy.text.GStringTemplateEngine;

import java.io.File;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.auth.BasicUserPrincipal;
import org.commonjava.aprox.audit.BasicSecuritySystem;
import org.commonjava.aprox.audit.SecurityAction;
import org.commonjava.aprox.audit.SimpleSecurityAction;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.subsys.flatfile.conf.DataFile;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileConfiguration;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileEventManager;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileManager;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
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

    private Principal principal;

    private BasicSecuritySystem security;

    @Before
    public void setup()
        throws Exception
    {
        storeManager = new MemoryStoreDataManager();

        final File dataSrc = new File( "./src/main/data" );
        final File data = temp.newFolder( "data" );
        FileUtils.copyDirectory( dataSrc, data );

        security = new BasicSecuritySystem();

        final DataFileConfiguration config = new DataFileConfiguration( data, temp.newFolder( "work" ) );
        final DataFileManager fileManager = new DataFileManager( config, new DataFileEventManager(), security );

        final TemplatingEngine templates = new TemplatingEngine( new GStringTemplateEngine(), fileManager );
        manager = new SetBackSettingsManager( storeManager, templates, fileManager );

        principal = new BasicUserPrincipal( "test-user" );
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

        final PrivilegedAction<Exception> action = new PrivilegedAction<Exception>()
        {
            @Override
            public Exception run()
            {
                try
                {
                    manager.deleteStoreSettings( key );
                }
                catch ( final SetBackDataException e )
                {
                    return e;
                }

                return null;
            }
        };

        final Exception error = security.runAs( principal, action );
        if ( error != null )
        {
            throw error;
        }

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
        final ProxyDataException error = security.runAs( principal, new PrivilegedAction<ProxyDataException>()
        {
            @Override
            public ProxyDataException run()
            {
                try
                {
                    storeManager.storeArtifactStore( store, "test" );
                }
                catch ( final ProxyDataException e )
                {
                    return e;
                }

                return null;
            }
        } );

        if ( error != null )
        {
            throw error;
        }
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
        final SecurityAction<List<String>, Exception> action = new SimpleSecurityAction<List<String>, Exception>()
        {
            @Override
            public List<String> run()
            {
                DataFile settings;
                try
                {
                    settings = manager.generateStoreSettings( key );
                }
                catch ( final SetBackDataException e )
                {
                    setError( e );
                    return null;
                }

                assertThat( "settings.xml returned from generateStoreSettings(..) for: " + key + " does not exist!",
                            settings.exists(), equalTo( true ) );

                List<String> lines;
                try
                {
                    lines = readSettings( key, true );
                }
                catch ( final Exception e )
                {
                    setError( e );
                    return null;
                }

                final String localRepoLine =
                    String.format( "<localRepository>%s</localRepository>",
                                   normalize( USER_HOME,
                                              ".m2/repository-" + key.getType()
                                                                     .singularEndpointName() + "-" + key.getName() ) );

                assertThat( "Local repository for: " + key + " not configured", lines.contains( localRepoLine ),
                            equalTo( true ) );

                return lines;
            }
        };

        final List<String> lines = security.runAs( principal, action );
        final Exception error = action.getError();
        if ( error != null )
        {
            throw error;
        }

        return lines;
    }

}
