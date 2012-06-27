package org.commonjava.aprox.core.rest.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.core.model.ArtifactStore;


public class EndpointViewListing
{

    private final Collection<EndpointView> items;

    public EndpointViewListing( final Collection<ArtifactStore> stores, final UriInfo uriInfo )
    {
        final List<EndpointView> points = new ArrayList<EndpointView>();
        for ( final ArtifactStore store : stores )
        {
            points.add( new EndpointView( store, uriInfo.getAbsolutePathBuilder() ) );
        }

        Collections.sort( points );

        this.items = Collections.unmodifiableSet( new LinkedHashSet<EndpointView>( points ) );
    }

    public Collection<EndpointView> getItems()
    {
        return items;
    }

}
