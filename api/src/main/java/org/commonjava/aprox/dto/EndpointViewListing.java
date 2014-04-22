/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.util.UriFormatter;

public class EndpointViewListing
{

    private final List<EndpointView> items;

    public EndpointViewListing( final Collection<ArtifactStore> stores, final String baseUri, final UriFormatter formatter )
    {
        final List<EndpointView> points = new ArrayList<EndpointView>();
        for ( final ArtifactStore store : stores )
        {
            final EndpointView point = new EndpointView( store, baseUri, formatter );
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
