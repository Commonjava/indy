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
