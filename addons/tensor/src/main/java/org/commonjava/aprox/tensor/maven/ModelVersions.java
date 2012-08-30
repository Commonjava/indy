package org.commonjava.aprox.tensor.maven;

import static org.apache.maven.artifact.ArtifactUtils.versionlessKey;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;

public class ModelVersions
{

    private final Map<String, Dependency> deps = new HashMap<String, Dependency>();

    private final Map<String, Dependency> managedDeps = new HashMap<String, Dependency>();

    private final Map<String, Extension> extensions = new HashMap<String, Extension>();

    private final Map<String, Plugin> plugins = new HashMap<String, Plugin>();

    private final Map<String, Dependency> pluginDeps = new HashMap<String, Dependency>();

    private final Map<String, Plugin> managedPlugins = new HashMap<String, Plugin>();

    private final Map<String, Dependency> managedPluginDeps = new HashMap<String, Dependency>();

    public ModelVersions( final Model effectiveModel )
    {
        map( effectiveModel );
    }

    private void map( final Model effectiveModel )
    {
        // map dependencies
        for ( final Dependency d : effectiveModel.getDependencies() )
        {
            deps.put( d.getManagementKey(), d );
        }

        // map managed dependencies
        final DependencyManagement dm = effectiveModel.getDependencyManagement();
        if ( dm != null )
        {
            for ( final Dependency d : dm.getDependencies() )
            {
                managedDeps.put( d.getManagementKey(), d );
            }
        }

        final Build build = effectiveModel.getBuild();

        // map plugins
        // map managed plugins
        // TODO: map report plugins
        // TODO: map site report plugins
        // map extensions
        // map managed plugin-level dependencies
        if ( build != null )
        {
            if ( build.getPlugins() != null )
            {
                for ( final Plugin plugin : build.getPlugins() )
                {
                    plugins.put( plugin.getKey(), plugin );
                    if ( plugin.getDependencies() != null )
                    {
                        for ( final Dependency dep : plugin.getDependencies() )
                        {
                            pluginDeps.put( pluginDepKey( plugin, dep ), dep );
                        }
                    }
                }
            }

            final PluginManagement pm = build.getPluginManagement();
            if ( pm != null )
            {
                if ( pm.getPlugins() != null )
                {
                    for ( final Plugin plugin : pm.getPlugins() )
                    {
                        managedPlugins.put( plugin.getKey(), plugin );
                        if ( plugin.getDependencies() != null )
                        {
                            for ( final Dependency dep : plugin.getDependencies() )
                            {
                                managedPluginDeps.put( pluginDepKey( plugin, dep ), dep );
                            }
                        }
                    }
                }
            }

            if ( build.getExtensions() != null )
            {
                for ( final Extension ext : build.getExtensions() )
                {
                    extensions.put( versionlessKey( ext.getGroupId(), ext.getArtifactId() ), ext );
                }
            }
        }
    }

    private String pluginDepKey( final Plugin plugin, final Dependency dep )
    {
        return plugin.getKey() + "#" + dep.getManagementKey();
    }

    public void update( final Model rawModel )
    {
        // iterate raw deps
        // iterate raw managed deps
        // iterate raw plugins
        // iterate raw plugin-level deps
        // iterate raw managed plugins
        // iterate raw managed plugin-level deps
        // iterate raw extensions
        // TODO: iterate report plugins
        // TODO: iterate site report plugins
        for ( final Dependency d : rawModel.getDependencies() )
        {
            final Dependency ed = deps.get( d.getManagementKey() );
            if ( ed != null )
            {
                d.setVersion( ed.getVersion() );
                d.setScope( ed.getScope() );
            }
        }

        // map managed dependencies
        final DependencyManagement dm = rawModel.getDependencyManagement();
        if ( dm != null )
        {
            for ( final Dependency d : dm.getDependencies() )
            {
                final Dependency ed = managedDeps.get( d.getManagementKey() );
                if ( ed != null )
                {
                    d.setVersion( ed.getVersion() );
                    d.setScope( ed.getScope() );
                }
            }
        }

        final Build build = rawModel.getBuild();

        // map plugins
        // map managed plugins
        // map extensions
        // map managed plugin-level dependencies
        if ( build != null )
        {
            if ( build.getPlugins() != null )
            {
                for ( final Plugin plugin : build.getPlugins() )
                {
                    final Plugin ep = plugins.get( plugin.getKey() );
                    if ( ep != null )
                    {
                        plugin.setVersion( ep.getVersion() );

                        if ( plugin.getDependencies() != null )
                        {
                            for ( final Dependency dep : plugin.getDependencies() )
                            {
                                final Dependency ed = pluginDeps.get( pluginDepKey( plugin, dep ) );
                                if ( ed != null )
                                {
                                    dep.setVersion( ed.getVersion() );
                                }
                            }
                        }
                    }
                }
            }

            final PluginManagement pm = build.getPluginManagement();
            if ( pm != null )
            {
                if ( pm.getPlugins() != null )
                {
                    for ( final Plugin plugin : pm.getPlugins() )
                    {
                        final Plugin ep = managedPlugins.get( plugin.getKey() );
                        if ( ep != null )
                        {
                            plugin.setVersion( ep.getVersion() );

                            if ( plugin.getDependencies() != null )
                            {
                                for ( final Dependency dep : plugin.getDependencies() )
                                {
                                    final Dependency ed = managedPluginDeps.get( pluginDepKey( plugin, dep ) );
                                    if ( ed != null )
                                    {
                                        dep.setVersion( ed.getVersion() );
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if ( build.getExtensions() != null )
            {
                for ( final Extension ext : build.getExtensions() )
                {
                    final Extension eext = extensions.get( versionlessKey( ext.getGroupId(), ext.getArtifactId() ) );
                    if ( eext != null )
                    {
                        ext.setVersion( eext.getVersion() );
                    }
                }
            }
        }
    }

}
