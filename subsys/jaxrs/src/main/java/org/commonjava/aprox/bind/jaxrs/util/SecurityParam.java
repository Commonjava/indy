package org.commonjava.aprox.bind.jaxrs.util;

public enum SecurityParam
{

    user;

    public String key()
    {
        return name();
    }

}
