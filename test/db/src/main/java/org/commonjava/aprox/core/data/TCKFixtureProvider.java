package org.commonjava.aprox.core.data;

import org.commonjava.aprox.core.model.ModelFactory;

public interface TCKFixtureProvider
{

    ProxyDataManager getDataManager();

    ModelFactory getModelFactory();

}
