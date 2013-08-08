package org.commonjava.aprox.depgraph.preset;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.filter.AnyFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;

@Named( "none" )
@ApplicationScoped
public class NonePresetFactory
    implements PresetFactory
{

    @Override
    public String getPresetId()
    {
        return "none";
    }

    @Override
    public ProjectRelationshipFilter newFilter( final GraphWorkspace workspace )
    {
        return new AnyFilter();
    }

}
