package org.commonjava.aprox.core.inject;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.change.dispatch.CouchChangeDispatcher;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.CouchAppReader;
import org.commonjava.couch.io.CouchHttpClient;
import org.commonjava.couch.io.Serializer;

@Singleton
public class AproxDataProviders
{

    @Inject
    private Serializer serializer;

    @Inject
    private CouchAppReader appReader;

    @Inject
    private CouchChangeDispatcher dispatcher;

    @Inject
    @AproxData
    private CouchDBConfiguration config;

    private CouchManager couchManager;

    private CouchHttpClient httpClient;

    private CouchChangeListener changeListener;

    @Produces
    @AproxData
    @Default
    public synchronized CouchChangeListener getChangeListener()
    {
        System.out.println( "Returning change listener for user DB" );
        if ( changeListener == null )
        {
            changeListener =
                new CouchChangeListener( dispatcher, getHttpClient(), config, getCouchManager(),
                                         serializer );
        }

        return changeListener;
    }

    @Produces
    @AproxData
    @Default
    public synchronized CouchManager getCouchManager()
    {
        if ( couchManager == null )
        {
            couchManager = new CouchManager( config, getHttpClient(), serializer, appReader );
        }

        return couchManager;
    }

    @Produces
    @AproxData
    @Default
    public synchronized CouchHttpClient getHttpClient()
    {
        if ( httpClient == null )
        {
            httpClient = new CouchHttpClient( config, serializer );
        }

        return httpClient;
    }

}
