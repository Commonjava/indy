/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.flat.data;

import java.io.File;
import java.security.PrivilegedAction;

import org.commonjava.aprox.audit.BasicSecuritySystem;
import org.commonjava.aprox.audit.SecuritySystem;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileConfiguration;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileEventManager;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileManager;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class FlatTCKFixtureProvider
    extends TemporaryFolder
    implements TCKFixtureProvider
{
    private TestFlatFileDataManager dataManager;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File configDir;

    private SecuritySystem securitySystem;

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

    @Override
    public SecuritySystem getSecuritySystem()
    {
        return securitySystem;
    }

    @Override
    protected void before()
        throws Throwable
    {
        super.before();

        configDir = newFolder( "db" );

        final JsonSerializer serializer = new JsonSerializer();

        securitySystem = new BasicSecuritySystem();

        dataManager =
            new TestFlatFileDataManager( new DataFileConfiguration().withDataBasedir( configDir ), serializer,
                                         securitySystem );

        final ProxyDataException error = securitySystem.runAsSystemUser( new PrivilegedAction<ProxyDataException>()
        {
            @Override
            public ProxyDataException run()
            {
                try
                {
                    dataManager.install();
                    dataManager.clear( "Setting up test" );
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

    private static final class TestFlatFileDataManager
        extends DataFileStoreDataManager
    {

        public TestFlatFileDataManager( final DataFileConfiguration config, final JsonSerializer serializer,
                                        final SecuritySystem securitySystem )
        {
            super( new DataFileManager( config, new DataFileEventManager(), securitySystem ), serializer,
                   securitySystem );
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
