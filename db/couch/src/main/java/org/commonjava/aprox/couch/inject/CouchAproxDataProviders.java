/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.couch.inject;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.db.CouchFactory;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.CouchHttpClient;
import org.commonjava.shelflife.inject.Shelflife;

@Singleton
public class CouchAproxDataProviders
{

    @Inject
    private CouchFactory factory;

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
            changeListener = factory.getChangeListener( config );
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
            couchManager = factory.getCouchManager( config );
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
            httpClient = factory.getHttpClient( config );
        }

        return httpClient;
    }

    @Produces
    @Default
    @Shelflife
    public synchronized CouchManager getCouch()
    {
        return couchManager;
    }

}
