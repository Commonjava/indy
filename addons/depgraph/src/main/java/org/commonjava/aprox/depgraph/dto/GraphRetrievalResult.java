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
package org.commonjava.aprox.depgraph.dto;

import java.util.LinkedList;

import javax.ws.rs.core.Response;

import org.commonjava.maven.atlas.graph.model.EProjectGraph;

public final class GraphRetrievalResult
{

    private final LinkedList<EProjectGraph> graphs;

    private final Response response;

    private final LinkedList<GAVWithPreset> specs;

    public GraphRetrievalResult( final LinkedList<GAVWithPreset> specs, final LinkedList<EProjectGraph> graphs )
    {
        this.specs = specs;
        this.graphs = graphs;
        this.response = null;
    }

    public GraphRetrievalResult( final LinkedList<GAVWithPreset> specs, final Response response )
    {
        this.specs = specs;
        this.graphs = null;
        this.response = response;
    }

    public GraphRetrievalResult( final Response response )
    {
        this.specs = null;
        this.graphs = null;
        this.response = response;
    }

    public LinkedList<GAVWithPreset> getSpecs()
    {
        return specs;
    }

    public LinkedList<EProjectGraph> getGraphs()
    {
        return graphs;
    }

    public Response getResponse()
    {
        return response;
    }

}
