package org.commonjava.aprox.tensor.web;

import javax.servlet.annotation.WebFilter;

import org.commonjava.tensor.web.base.filter.WorkspaceHandlerFilter;

@WebFilter( { "/api/1.0/depgraph", "/api/1.0/depgraph/*" } )
public class AproxTensorWSHandlerFilter
    extends WorkspaceHandlerFilter
{

}
