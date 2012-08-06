package org.commonjava.aprox.core.conf;

public interface AproxConfigSection<T>
{

    Class<T> getConfigurationClass();

    String getSectionName();

}
