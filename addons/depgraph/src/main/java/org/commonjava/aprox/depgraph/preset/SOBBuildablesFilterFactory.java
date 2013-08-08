package org.commonjava.aprox.depgraph.preset;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.preset.SOBBuildablesFilter;

@Named( "sob-build" )
@ApplicationScoped
public class SOBBuildablesFilterFactory
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
        return "sob-build";
    }
}