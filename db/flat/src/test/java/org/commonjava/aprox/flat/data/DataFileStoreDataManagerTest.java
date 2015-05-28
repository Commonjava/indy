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
package org.commonjava.aprox.flat.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.core.data.testutil.StoreEventDispatcherStub;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.commonjava.aprox.subsys.datafile.conf.DataFileConfiguration;
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

        mgr = new DataFileStoreDataManager( fileMgr, new AproxObjectMapper( false ), new StoreEventDispatcherStub() );
    }

    @Test
    public void getDataFileReturnsRemoteRepoJsonFile()
        throws Exception
    {
        final String name = "foo";
        final boolean success =
            mgr.storeArtifactStore( new RemoteRepository( name, "http://www.foo.com/" ),
                                       new ChangeSummary( "test-user", "init" ) );

        assertThat( success, equalTo( true ) );

        final DataFile dataFile = mgr.getDataFile( new StoreKey( StoreType.remote, name ) );
        assertThat( dataFile.getDetachedFile()
                            .getAbsolutePath(), equalTo( new File( fileCfg.getDataBasedir(), "aprox/remote/" + name
            + ".json" ).getAbsolutePath() ) );
    }

}
