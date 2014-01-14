package org.commonjava.aprox.bind.vertx.util;

public enum PathParam
{

    name, path, type;

    private String key;

    private PathParam()
    {
    }

    private PathParam( final String key )
    {
        this.key = key;
    }

    public String key()
    {
        if ( key == null )
        {
            return name();
        }

        return key;
    }

}
