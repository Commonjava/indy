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
package org.commonjava.aprox.core.rest.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.model.ArtifactStore;

public class EndpointViewListing
{

    private final List<EndpointView> items;

    public EndpointViewListing( final Collection<ArtifactStore> stores, final UriInfo uriInfo )
    {
        final List<EndpointView> points = new ArrayList<EndpointView>();
        for ( final ArtifactStore store : stores )
        {
            final EndpointView point = new EndpointView( store, uriInfo.getAbsolutePathBuilder() );
            if ( !points.contains( point ) )
            {
                points.add( point );
            }
        }

        Collections.sort( points );

        this.items = points;
    }

    public List<EndpointView> getItems()
    {
        return items;
    }

}
