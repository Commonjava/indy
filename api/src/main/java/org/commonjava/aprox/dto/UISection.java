package org.commonjava.aprox.dto;

public class UISection
{

    private String name;

    private String route;

    private String templateHref;

    private String controller;

    public UISection()
    {
    }

    public UISection( final String name, final String route, final String template, final String controller )
    {
        this.name = name;
        this.route = route;
        this.templateHref = template;
        this.controller = controller;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public UISection withName( final String name )
    {
        this.name = name;
        return this;
    }

    public String getRoute()
    {
        return route;
    }

    public void setRoute( final String route )
    {
        this.route = route;
    }

    public UISection withRoute( final String route )
    {
        this.route = route;
        return this;
    }

    public String getController()
    {
        return controller;
    }

    public void setController( final String controller )
    {
        this.controller = controller;
    }

    public UISection withController( final String controller )
    {
        this.controller = controller;
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

    public UISection withTemplateHref( final String templateHref )
    {
        this.templateHref = templateHref;
        return this;
    }

}
