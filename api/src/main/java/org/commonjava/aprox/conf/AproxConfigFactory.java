package org.commonjava.aprox.conf;

import org.commonjava.web.config.ConfigurationException;

public interface AproxConfigFactory
{

    <T> T getConfiguration( Class<T> configCls )
        throws ConfigurationException;

}