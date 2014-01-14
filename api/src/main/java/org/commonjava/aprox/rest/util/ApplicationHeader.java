package org.commonjava.aprox.rest.util;

public enum ApplicationHeader
{

    content_type( "Content-Type" ), location( "Location" ), uri( "URI" );

    private final String key;

    private ApplicationHeader( final String key )
    {
        this.key = key;
    }

    public String key()
    {
        return key;
    }

}
