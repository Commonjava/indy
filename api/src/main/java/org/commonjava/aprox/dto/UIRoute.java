package org.commonjava.aprox.dto;

public class UIRoute
{

    private String route;

    private String templateHref;

    public UIRoute()
    {
    }

    public UIRoute( final String route, final String templateHref )
    {
        this.route = route;
        this.templateHref = templateHref;
    }

    public String getRoute()
    {
        return route;
    }

    public void setRoute( final String route )
    {
        this.route = route;
    }

    public UIRoute withRoute( final String route )
    {
        this.route = route;
        return this;
    }

    public String getTemplateHref()
    {
        return templateHref;
    }

    public void setTemplateHref( final String templateHref )
    {
        this.templateHref = templateHref;
    }

    public UIRoute withTemplateHref( final String templateHref )
    {
        this.templateHref = templateHref;
        return this;
    }

}
