package org.commonjava.aprox.depbase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.commonjava.aprox.core.change.event.FileStorageEvent;
import org.commonjava.aprox.core.change.event.FileStorageEvent.Type;
import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.aprox.depbase.maven.ArtifactStoreModelResolver;
import org.commonjava.depbase.data.DepbaseDataException;
import org.commonjava.depbase.data.DepbaseDataManager;
import org.commonjava.depbase.model.DependencyRelationship;
import org.commonjava.depbase.model.ProjectId;
import org.commonjava.depbase.model.ProjectMetadata;
import org.commonjava.util.logging.Logger;

@Singleton
public class DepBaseProxyListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private DepbaseDataManager depbase;

    @Inject
    private ProxyDataManager aprox;

    @Inject
    private FileManager fileManager;

    @Inject
    @Named( "MAE" )
    private ModelBuilder modelBuilder;

    public void handleFileEvent( @Observes final FileStorageEvent event )
    {
        if ( Type.GENERATE == event.getType() )
        {
            return;
        }

        ArtifactStore originatingStore = event.getStore();
        List<ArtifactStore> stores = getRelevantStores( originatingStore );
        if ( stores == null )
        {
            return;
        }

        Model model = loadModel( event, stores );
        if ( model == null )
        {
            return;
        }

        ProjectId id =
            new ProjectId( model.getGroupId(), model.getArtifactId(), model.getVersion() );

        ProjectMetadata pm = new ProjectMetadata( id );

        storeParentRelationship( id, pm, model, event );
        storeDependencyRelationships( id, pm, model, event );
        storePluginRelationships( id, pm, model, event );

        try
        {
            depbase.storeProjectMetadata( pm );
        }
        catch ( DepbaseDataException e )
        {
            logger.error( "Cannot store project summary metadata for POM: %s. Reason: %s", e,
                          event.getPath(), e.getMessage() );
        }
    }

    protected void storePluginRelationships( final ProjectId id, final ProjectMetadata pm,
                                             final Model model, final FileStorageEvent event )
    {
        List<Plugin> plugins =
            model.getBuild() == null ? new ArrayList<Plugin>() : model.getBuild().getPlugins();

        List<ProjectId> pluginIds = new ArrayList<ProjectId>( plugins.size() );
        for ( Plugin plugin : plugins )
        {
            if ( plugin == null )
            {
                continue;
            }

            pluginIds.add( new ProjectId( plugin.getGroupId(), plugin.getArtifactId(),
                                          plugin.getVersion() ) );
        }

        try
        {
            depbase.storePluginUsages( id, pluginIds );
        }
        catch ( DepbaseDataException e )
        {
            logger.error( "Cannot store %d project relationships (plugin usages) for POM: %s. Reason: %s",
                          e, pluginIds.size(), event.getPath(), e.getMessage() );
        }
        pm.setPluginCount( pluginIds.size() );
    }

    protected void storeDependencyRelationships( final ProjectId id, final ProjectMetadata pm,
                                                 final Model model, final FileStorageEvent event )
    {
        List<Dependency> deps = model.getDependencies();
        List<DependencyRelationship> depRels = new ArrayList<DependencyRelationship>( deps.size() );
        int idx = 0;
        for ( Dependency dep : deps )
        {
            if ( dep == null )
            {
                continue;
            }

            ProjectId did = new ProjectId( dep.getGroupId(), dep.getArtifactId(), dep.getVersion() );
            depRels.add( new DependencyRelationship( id, did, dep.getType(), dep.getClassifier(),
                                                     dep.getScope(), idx ) );
            idx++;
        }

        try
        {
            depbase.storeDependencies( depRels );
        }
        catch ( DepbaseDataException e )
        {
            logger.error( "Cannot store %d project relationships (dependencies) for POM: %s. Reason: %s",
                          e, depRels.size(), event.getPath(), e.getMessage() );
        }
        pm.setDependencyCount( depRels.size() );
    }

    protected void storeParentRelationship( final ProjectId id, final ProjectMetadata pm,
                                            final Model model, final FileStorageEvent event )
    {
        Parent parent = model.getParent();
        if ( parent != null )
        {
            ProjectId parentId =
                new ProjectId( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
            pm.setParent( parentId );

            try
            {
                depbase.storeParent( id, parentId );
            }
            catch ( DepbaseDataException e )
            {
                logger.error( "Cannot store project relationships (parent) for POM: %s. Reason: %s",
                              e, event.getPath(), e.getMessage() );
            }
        }
    }

    protected Model loadModel( final FileStorageEvent event, final List<ArtifactStore> stores )
    {
        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile( new File( event.getStorageLocation() ) );
        request.setModelResolver( new ArtifactStoreModelResolver( fileManager, stores ) );

        ModelBuildingResult result = null;
        try
        {
            result = modelBuilder.build( request );
        }
        catch ( ModelBuildingException e )
        {
            logger.error( "Cannot build model instance for POM: %s. Reason: %s", e,
                          event.getPath(), e.getMessage() );
        }

        if ( result == null )
        {
            return null;
        }

        return result.getEffectiveModel();
    }

    protected List<ArtifactStore> getRelevantStores( final ArtifactStore originatingStore )
    {
        List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        stores.add( originatingStore );

        try
        {
            Set<Group> groups = aprox.getGroupsContaining( originatingStore.getKey() );
            for ( Group group : groups )
            {
                if ( group == null )
                {
                    continue;
                }

                List<ArtifactStore> orderedStores =
                    aprox.getOrderedConcreteStoresInGroup( group.getName() );
                if ( orderedStores != null )
                {
                    for ( ArtifactStore as : orderedStores )
                    {
                        if ( as == null || stores.contains( as ) )
                        {
                            continue;
                        }

                        stores.add( as );
                    }
                }
            }
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Cannot lookup full store list for groups containing artifact store: %s. Reason: %s",
                          e, originatingStore.getKey(), e.getMessage() );
            stores = null;
        }

        return stores;
    }
}
