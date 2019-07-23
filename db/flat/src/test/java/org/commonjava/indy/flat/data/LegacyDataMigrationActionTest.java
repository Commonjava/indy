/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.flat.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.core.data.testutil.StoreEventDispatcherStub;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.commonjava.indy.flat.data.DataFileStoreUtils.INDY_STORE;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@Ignore("Obsolete")
public class LegacyDataMigrationActionTest
{

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private DataFileManager dfm;

    private LegacyDataMigrationAction action;

    @Before
    public void setup()
            throws Exception
    {
        dfm = new DataFileManager( temp.newFolder( "stores" ), new DataFileEventManager() );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        final StoreEventDispatcher sed = new StoreEventDispatcherStub();

        action = new LegacyDataMigrationAction(
                dfm, new DataFileStoreDataManager( dfm, mapper, sed ), mapper );
    }

    @Test
    public void migrateGroupJSONWithMissingTypeAttribute()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, group.singularEndpointName() );

        dir.mkdirs();

        DataFile in = dfm.getDataFile( INDY_STORE, group.singularEndpointName(), "test.json" );
        in.writeString( "{\"name\": \"test\", \"packageType\": \"maven\", \"key\": \"maven:group:test\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test group creation" ) );

        System.out.println( "Wrote: " + in.getPath() );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        DataFile out = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, group.singularEndpointName(), "test.json" );
        final String json = out.readString();
        assertThat( json.contains( "\"type\" : \"group\"" ), equalTo( true ) );
        assertThat( json.contains( "\"packageType\" : \"maven\"" ), equalTo( true ) );
        assertThat( json.contains( "\"key\" : \"maven:group:test\"" ), equalTo( true ) );
    }

    @Test
    public void dataFileMigrationForGroupJSONWithExistingTypeAttribute()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, group.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"packageType\": \"maven\", \"type\" : \"group\", \"key\": \"maven:group:test\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test group creation" ) );

        String json = file.readString();
        assertThat( json.contains( "\"type\" : \"group\"" ), equalTo( true ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        DataFile out = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, group.singularEndpointName(), "test.json" );
        json = out.readString();
        assertThat( json.contains( "\"type\" : \"group\"" ), equalTo( true ) );
        assertThat( json.contains( "\"packageType\" : \"maven\"" ), equalTo( true ) );
        assertThat( json.contains( "\"key\" : \"maven:group:test\"" ), equalTo( true ) );
    }

    @Test
    public void migrateHostedRepoJSONWithMissingTypeAttribute()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, hosted.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"packageType\": \"maven\", \"key\": \"maven:hosted:test\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        DataFile out = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, hosted.singularEndpointName(), "test.json" );
        final String json = out.readString();
        assertThat( json.contains( "\"type\" : \"hosted\"" ), equalTo( true ) );
        assertThat( json.contains( "\"packageType\" : \"maven\"" ), equalTo( true ) );
        assertThat( json.contains( "\"key\" : \"maven:hosted:test\"" ), equalTo( true ) );
    }

    @Test
    public void dataFileMigrationForHostedRepoJSONWithExistingTypeAttribute()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, hosted.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"type\" : \"hosted\", \"packageType\": \"maven\", \"key\": \"maven:hosted:test\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        String json = file.readString();
        assertThat( json.contains( "\"type\" : \"hosted\"" ), equalTo( true ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        DataFile out = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, hosted.singularEndpointName(), "test.json" );
        json = out.readString();
        assertThat( json.contains( "\"type\" : \"hosted\"" ), equalTo( true ) );
        assertThat( json.contains( "\"packageType\" : \"maven\"" ), equalTo( true ) );
        assertThat( json.contains( "\"key\" : \"maven:hosted:test\"" ), equalTo( true ) );
    }

    @Test
    public void dataFileMigrationForExistingHostedRepoJSON()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, hosted.singularEndpointName() );

        dir.mkdirs();

        final DataFile old = dfm.getDataFile( INDY_STORE, hosted.singularEndpointName(), "test.json" );
        final DataFile migrated = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, hosted.singularEndpointName(), "test.json" );

        String srcJson = "{\"name\": \"test\", \"type\" : \"hosted\", \"packageType\": \"maven\", \"key\": \"maven:hosted:test\"}";

        old.writeString( srcJson, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );
        migrated.writeString( srcJson, new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( false ) );
    }

    @Test
    public void migrateRemoteRepoJSONWithMissingTypeAttribute()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, remote.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"packageType\": \"maven\", \"key\": \"maven:remote:test\", \"url\": \"http://www.google.com\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        DataFile out = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, remote.singularEndpointName(), "test.json" );
        final String json = out.readString();
        assertThat( json.contains( "\"type\" : \"remote\"" ), equalTo( true ) );
        assertThat( json.contains( "\"packageType\" : \"maven\"" ), equalTo( true ) );
        assertThat( json.contains( "\"key\" : \"maven:remote:test\"" ), equalTo( true ) );
    }

    @Test
    public void migrateRemoteRepoJSONWithMissingPackageTypeInKey()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, remote.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString( "{\"name\": \"test\", \"type\": \"remote\", \"key\": \"remote:test\", \"url\": \"http://www.google.com\"}",
                          new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        DataFile out = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, remote.singularEndpointName(), "test.json" );
        final String json = out.readString();
        assertThat( json.contains( "\"type\" : \"remote\"" ), equalTo( true ) );
        assertThat( json.contains( "\"packageType\" : \"maven\"" ), equalTo( true ) );
        assertThat( json.contains( "\"key\" : \"maven:remote:test\"" ), equalTo( true ) );
    }

    @Test
    public void dataFileMigrationForRemoteRepoJSONWithExistingTypeAttribute()
            throws Exception
    {
        final DataFile dir =
                dfm.getDataFile( INDY_STORE, remote.singularEndpointName() );

        dir.mkdirs();

        final DataFile file = dir.getChild( "test.json" );
        file.writeString(
                "{\"name\": \"test\", \"type\" : \"remote\", \"packageType\": \"maven\", \"key\": \"maven:remote:test\", \"url\": \"http://www.google.com\"}",
                new ChangeSummary( ChangeSummary.SYSTEM_USER, "test repo creation" ) );

        final boolean result = action.migrate();

        assertThat( result, equalTo( true ) );

        DataFile out = dfm.getDataFile( INDY_STORE, MAVEN_PKG_KEY, remote.singularEndpointName(), "test.json" );
        String json = out.readString();
        assertThat( json.contains( "\"type\" : \"remote\"" ), equalTo( true ) );
        assertThat( json.contains( "\"packageType\" : \"maven\"" ), equalTo( true ) );
        assertThat( json.contains( "\"key\" : \"maven:remote:test\"" ), equalTo( true ) );
    }

}
