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

import java.io.File;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.core.data.TCKFixtureProvider;
import org.commonjava.indy.core.data.testutil.StoreEventDispatcherStub;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.datafile.conf.DataFileConfiguration;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DataFileTCKFixtureProvider
        extends TemporaryFolder
        implements TCKFixtureProvider
{
    private TestFlatFileDataManager dataManager;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File configDir;

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

    @Override
    protected void before()
            throws Throwable
    {
        super.before();

        configDir = newFolder( "db" );

        final IndyObjectMapper serializer = new IndyObjectMapper( true );

        dataManager =
                new TestFlatFileDataManager( new DataFileConfiguration().withDataBasedir( configDir ), serializer );

        dataManager.install();
        dataManager.clear( new ChangeSummary( ChangeSummary.SYSTEM_USER, "Setting up test" ) );
    }

    private static final class TestFlatFileDataManager
            extends DataFileStoreDataManager
    {

        public TestFlatFileDataManager( final DataFileConfiguration config, final IndyObjectMapper serializer )
        {
            super( new DataFileManager( config, new DataFileEventManager() ), serializer,
                   new StoreEventDispatcherStub() );
        }

        //        @Override
        //        public HostedRepository getHostedRepository( final String name )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getHostedRepository( name );
        //        }
        //
        //        @Override
        //        public RemoteRepository getRemoteRepository( final String name )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getRemoteRepository( name );
        //        }
        //
        //        @Override
        //        public Group getGroup( final String name )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getGroup( name );
        //        }
        //
        //        @Override
        //        public List<Group> getAllGroups()
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getAllGroups();
        //        }
        //
        //        @Override
        //        public List<RemoteRepository> getAllRemoteRepositories()
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getAllRemoteRepositories();
        //        }
        //
        //        @Override
        //        public List<HostedRepository> getAllHostedRepositories()
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getAllHostedRepositories();
        //        }
        //
        //        @Override
        //        public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getOrderedConcreteStoresInGroup( groupName );
        //        }
        //
        //        @Override
        //        public Set<Group> getGroupsContaining( final StoreKey repo )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getGroupsContaining( repo );
        //        }
        //
        //        @Override
        //        public void install()
        //            throws ProxyDataException
        //        {
        //            getDataManager().install();
        //        }
        //
        //        @Override
        //        public void clear()
        //            throws ProxyDataException
        //        {
        //            getDataManager().clear();
        //        }
        //
        //        @Override
        //        public ArtifactStore getArtifactStore( final StoreKey key )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getArtifactStore( key );
        //        }
        //
        //        @Override
        //        public List<ArtifactStore> getAllArtifactStores()
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getAllArtifactStores();
        //        }
        //
        //        @Override
        //        public List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getOrderedStoresInGroup( groupName );
        //        }
        //
        //        @Override
        //        public List<? extends ArtifactStore> getAllArtifactStores( final StoreType type )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getAllArtifactStores( type );
        //        }
        //
        //        @Override
        //        public boolean storeArtifactStore( final ArtifactStore key )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().storeArtifactStore( key );
        //        }
        //
        //        @Override
        //        public boolean storeArtifactStore( final ArtifactStore key, final boolean skipIfExists )
        //            throws ProxyDataException
        //        {
        //            return getDataManager().storeArtifactStore( key, skipIfExists );
        //        }
        //
        //        @Override
        //        public void deleteArtifactStore( final StoreKey key )
        //            throws ProxyDataException
        //        {
        //            getDataManager().deleteArtifactStore( key );
        //        }
        //
        //        @Override
        //        public boolean hasRemoteRepository( final String name )
        //        {
        //            return getDataManager().hasRemoteRepository( name );
        //        }
        //
        //        @Override
        //        public boolean hasGroup( final String name )
        //        {
        //            return getDataManager().hasGroup( name );
        //        }
        //
        //        @Override
        //        public boolean hasHostedRepository( final String name )
        //        {
        //            return getDataManager().hasHostedRepository( name );
        //        }
        //
        //        @Override
        //        public boolean hasArtifactStore( final StoreKey key )
        //        {
        //            return getDataManager().hasArtifactStore( key );
        //        }
        //
        //        @Override
        //        public List<ArtifactStore> getAllConcreteArtifactStores()
        //            throws ProxyDataException
        //        {
        //            return getDataManager().getAllConcreteArtifactStores();
        //        }
        //
    }

}
