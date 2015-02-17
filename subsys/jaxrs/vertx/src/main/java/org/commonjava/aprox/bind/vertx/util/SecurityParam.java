package org.commonjava.aprox.bind.vertx.util;

public enum SecurityParam
{

    user;

    public String key()
    {
        return name();
    }

}
