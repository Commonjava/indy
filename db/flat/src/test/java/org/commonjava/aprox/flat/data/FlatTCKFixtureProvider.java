package org.commonjava.aprox.flat.data;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.flat.conf.FlatFileConfiguration;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
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

        configDir = newFolder( "definitions" );

        final JsonSerializer serializer = new JsonSerializer();

        dataManager = new TestFlatFileDataManager( new FlatFileConfiguration( configDir ), serializer );

        dataManager.install();
    }

    private static final class TestFlatFileDataManager
        extends FlatFileDataManagerDecorator
    {

        public TestFlatFileDataManager( final FlatFileConfiguration config, final JsonSerializer serializer )
        {
            super( new MemoryStoreDataManager(), config, serializer );
        }

        @Override
        public DeployPoint getDeployPoint( final String name )
            throws ProxyDataException
        {
            return getDataManager().getDeployPoint( name );
        }

        @Override
        public Repository getRepository( final String name )
            throws ProxyDataException
        {
            return getDataManager().getRepository( name );
        }

        @Override
        public Group getGroup( final String name )
            throws ProxyDataException
        {
            return getDataManager().getGroup( name );
        }

        @Override
        public List<Group> getAllGroups()
            throws ProxyDataException
        {
            return getDataManager().getAllGroups();
        }

        @Override
        public List<Repository> getAllRepositories()
            throws ProxyDataException
        {
            return getDataManager().getAllRepositories();
        }

        @Override
        public List<DeployPoint> getAllDeployPoints()
            throws ProxyDataException
        {
            return getDataManager().getAllDeployPoints();
        }

        @Override
        public List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
            throws ProxyDataException
        {
            return getDataManager().getOrderedConcreteStoresInGroup( groupName );
        }

        @Override
        public Set<Group> getGroupsContaining( final StoreKey repo )
            throws ProxyDataException
        {
            return getDataManager().getGroupsContaining( repo );
        }

        @Override
        public void install()
            throws ProxyDataException
        {
            getDataManager().install();
        }

        @Override
        public ArtifactStore getArtifactStore( final StoreKey key )
            throws ProxyDataException
        {
            return getDataManager().getArtifactStore( key );
        }

        @Override
        public List<ArtifactStore> getAllArtifactStores()
            throws ProxyDataException
        {
            return getDataManager().getAllArtifactStores();
        }

    }

}
