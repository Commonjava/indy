package org.commonjava.aprox.depbase.inject;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.depbase.inject.DepbaseData;

@Singleton
public class DepbaseAproxProviders
{

    @Produces
    @DepbaseData
    @Default
    public CouchDBConfiguration getDepbaseDatabaseConfig()
    {
        return null;
    }

}
