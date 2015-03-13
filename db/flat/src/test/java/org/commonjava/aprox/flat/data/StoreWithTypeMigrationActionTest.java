package org.commonjava.aprox.flat.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.core.data.testutil.StoreEventDispatcherStub;
import org.commonjava.aprox.data.StoreEventDispatcher;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class StoreWithTypeMigrationActionTest
{

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private DataFileManager dfm;

    private StoreWithTypeMigrationAction action;

    private AproxObjectMapper mapper;

    @Before
    public void setup()
        throws Exception
    {
        dfm = new DataFileManager( temp.newFolder( "stores" ), new DataFileEventManager() );

        mapper = new AproxObjectMapper( true );

        final StoreEventDispatcher sed = new StoreEventDispatcherStub();

        action = new StoreWithTypeMigrationAction( new DataFileStoreDataManager( dfm, mapper, sed ) );
    }

    @Test
    public void migrateGroupJSONWithMissingTypeAttribute()
        throws Exception
    {
        final DataFile dir =
            dfm.getDataFile( DataFileStoreDataManager.APROX_STORE, StoreType.group.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"key\": \"group:test\"}",
                               new ChangeSummary( ChangeSummary.SYSTEM_USER, "test group creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        final String migratedJson = file.readString();
        assertThat( migratedJson.contains( "\"type\" : \"group\"" ), equalTo( true ) );
    }

    @Test
    public void noMigrationForGroupJSONWithExistingTypeAttribute()
        throws Exception
    {
        final DataFile dir =
            dfm.getDataFile( DataFileStoreDataManager.APROX_STORE, StoreType.group.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"type\" : \"group\", \"key\": \"group:test\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test group creation" ) );

        String json = file.readString();
        assertThat( json.contains( "\"type\" : \"group\"" ), equalTo( true ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( false ) );

        json = file.readString();
        assertThat( json.contains( "\"type\" : \"group\"" ), equalTo( true ) );
    }

    @Test
    public void migrateHostedRepoJSONWithMissingTypeAttribute()
        throws Exception
    {
        final DataFile dir =
            dfm.getDataFile( DataFileStoreDataManager.APROX_STORE, StoreType.hosted.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"key\": \"hosted:test\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        final String migratedJson = file.readString();
        assertThat( migratedJson.contains( "\"type\" : \"hosted\"" ), equalTo( true ) );
    }

    @Test
    public void noMigrationForHostedRepoJSONWithExistingTypeAttribute()
        throws Exception
    {
        final DataFile dir =
            dfm.getDataFile( DataFileStoreDataManager.APROX_STORE, StoreType.hosted.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"type\" : \"hosted\", \"key\": \"hosted:test\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        String json = file.readString();
        assertThat( json.contains( "\"type\" : \"hosted\"" ), equalTo( true ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( false ) );

        json = file.readString();
        assertThat( json.contains( "\"type\" : \"hosted\"" ), equalTo( true ) );
    }

    @Test
    public void migrateRemoteRepoJSONWithMissingTypeAttribute()
        throws Exception
    {
        final DataFile dir =
            dfm.getDataFile( DataFileStoreDataManager.APROX_STORE, StoreType.remote.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"key\": \"remote:test\", \"url\": \"http://www.google.com\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        final String migratedJson = file.readString();
        assertThat( migratedJson.contains( "\"type\" : \"remote\"" ), equalTo( true ) );
    }

    @Test
    public void noMigrationForRemoteRepoJSONWithExistingTypeAttribute()
        throws Exception
    {
        final DataFile dir =
            dfm.getDataFile( DataFileStoreDataManager.APROX_STORE, StoreType.remote.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"type\" : \"remote\", \"key\": \"remote:test\", \"url\": \"http://www.google.com\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        String json = file.readString();
        assertThat( json.contains( "\"type\" : \"remote\"" ), equalTo( true ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( false ) );

        json = file.readString();
        assertThat( json.contains( "\"type\" : \"remote\"" ), equalTo( true ) );
    }

}
