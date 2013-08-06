package org.commonjava.aprox.depgraph.util;

import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class VariableTargetFilter
    implements ProjectRelationshipFilter
{

    private final ProjectRelationshipFilter delegate;

    public VariableTargetFilter( final ProjectRelationshipFilter delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        return rel.getTarget()
                  .isVariableVersion() && delegate.accept( rel );
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> parent )
    {
        return new VariableTargetFilter( delegate.getChildFilter( parent ) );
    }

    @Override
    public void render( final StringBuilder sb )
    {
        sb.append( "variable targets within( " );
        delegate.render( sb );
        sb.append( " )" );
    }

}
