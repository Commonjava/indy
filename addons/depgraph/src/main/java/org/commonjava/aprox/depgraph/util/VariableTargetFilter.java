/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
