package org.commonjava.aprox.depgraph.preset;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.preset.ShippingOrientedBuildFilter;

@Named( "sob" )
@ApplicationScoped
public class ShippingOrientedBuildFilterFactory
    implements PresetFactory
{
    @Override
    public ProjectRelationshipFilter newFilter( final GraphWorkspace workspace )
    {
        return new ShippingOrientedBuildFilter();
    }

    @Override
    public String getPresetId()
    {
        return "sob";
    }
}