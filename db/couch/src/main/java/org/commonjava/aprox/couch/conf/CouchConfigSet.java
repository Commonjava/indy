package org.commonjava.aprox.couch.conf;

import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.AproxConfigSet;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;

@Singleton
public final class CouchConfigSet
    extends AproxConfigSet<CouchDBConfiguration, DefaultCouchDBConfiguration>
{
    public CouchConfigSet()
    {
        super( DefaultCouchDBConfiguration.class, "db-couch" );
    }

}