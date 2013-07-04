package org.commonjava.aprox.tensor.web;

import javax.servlet.annotation.WebFilter;

import org.commonjava.tensor.web.base.filter.WorkspaceHandlerFilter;

@WebFilter( { "/api/1.0/rel", "/api/1.0/rel/*" } )
public class AproxTensorWSHandlerFilter
    extends WorkspaceHandlerFilter
{

}
