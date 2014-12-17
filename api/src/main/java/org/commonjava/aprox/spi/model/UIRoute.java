package org.commonjava.aprox.spi.model;

/**
 * Describes a "route" in the UI, mainly useful for add-ons to register UI additions. This specifies a route (path) and template href, which corresponds
 * to an html fragment, potentially with UI-specific logic, that handles content display for that route. Any UI-side controller init/logic is assumed
 * to be embedded in the template.
 */
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
