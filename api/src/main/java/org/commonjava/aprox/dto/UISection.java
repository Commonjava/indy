package org.commonjava.aprox.dto;

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
