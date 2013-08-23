package org.commonjava.aprox.depgraph.preset;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.preset.SOBBuildablesFilter;

@Named( "managed-sob-build" )
@ApplicationScoped
public class ManagedSOBBuildablesFilterFactory
    implements PresetFactory
{
    @Override
    public ProjectRelationshipFilter newFilter( final GraphWorkspace workspace )
    {
        return new SOBBuildablesFilter();
    }

    @Override
    public String getPresetId()
    {
        return "managed-sob-build";
    }
}