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

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.core.data.testutil.StoreEventDispatcherStub;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.datafile.conf.DataFileConfiguration;
import org.commonjava.maven.galley.event.EventMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DataFileStoreDataManagerTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private DataFileStoreDataManager mgr;

    private DataFileConfiguration fileCfg;

    @Before
    public void setup()
            throws Exception
    {
        fileCfg = new DataFileConfiguration( temp.newFolder( "data" ), temp.newFolder( "work" ) );

        final DataFileManager fileMgr = new DataFileManager( fileCfg, new DataFileEventManager() );

        mgr = new DataFileStoreDataManager( fileMgr, new IndyObjectMapper( false ), new StoreEventDispatcherStub() );
    }

    @Test
    public void getDataFileReturnsRemoteRepoJsonFile()
            throws Exception
    {
        final String name = "foo";
        final boolean success = mgr.storeArtifactStore( new RemoteRepository( MAVEN_PKG_KEY, name, "http://www.foo.com/" ),
                                                        new ChangeSummary( "test-user", "init" ), false, true, new EventMetadata() );

        assertThat( success, equalTo( true ) );

        final DataFile dataFile = mgr.getDataFile( new StoreKey( StoreType.remote, name ) );
        assertThat( dataFile.getDetachedFile().getAbsolutePath(), equalTo(
                new File( fileCfg.getDataBasedir(), "indy/remote/" + name + ".json" ).getAbsolutePath() ) );
    }

}
