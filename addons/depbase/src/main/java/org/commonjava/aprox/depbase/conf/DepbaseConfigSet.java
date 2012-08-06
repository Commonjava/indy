package org.commonjava.aprox.depbase.conf;

import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.AproxConfigSet;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;

@Singleton
public final class DepbaseConfigSet
    extends AproxConfigSet<CouchDBConfiguration, DefaultCouchDBConfiguration>
{
    public DepbaseConfigSet()
    {
        super( DefaultCouchDBConfiguration.class, "db-couch" );
    }

}