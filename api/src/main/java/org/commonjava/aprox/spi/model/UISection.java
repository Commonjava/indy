package org.commonjava.aprox.spi.model;

/**
 * Describes a section of the UI, to allow add-ons to auto-register UI additions. This corresponds to a menu title (name) and a UI route (path) for
 * display in the UI menuing system. The route is used as a key that links this section to a corresponding {@link UIRoute} via the UI logic.
 */
public class UISection
{

    private String name;

    private String route;

    public UISection()
    {
    }

    public UISection( final String name, final String route )
    {
        this.name = name;
        this.route = route;
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

}
