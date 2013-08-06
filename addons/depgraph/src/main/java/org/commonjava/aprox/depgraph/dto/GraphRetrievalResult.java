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
