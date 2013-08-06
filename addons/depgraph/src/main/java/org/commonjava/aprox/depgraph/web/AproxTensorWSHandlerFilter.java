package org.commonjava.aprox.depgraph.web;

import javax.servlet.annotation.WebFilter;

import org.commonjava.aprox.depgraph.filter.WorkspaceHandlerFilter;

@WebFilter( { "/api/1.0/depgraph", "/api/1.0/depgraph/*" } )
public class AproxTensorWSHandlerFilter
    extends WorkspaceHandlerFilter
{

}
