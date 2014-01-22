/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.bind.vertx.util;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.ser.JsonSerializer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@ApplicationScoped
public class RequestSerialHelper
{

    private static final long READ_TIMEOUT = TimeUnit.MILLISECONDS.convert( 10, TimeUnit.SECONDS );

    @Inject
    @AproxData
    private JsonSerializer restSerializer;

    public ArtifactStore storeFromRequestBody( final StoreType st, final HttpServerRequest request )
    {
        switch ( st )
        {
            case deploy_point:
            {
                return fromRequestBody( request, restSerializer, DeployPoint.class );
            }
            case group:
            {
                return fromRequestBody( request, restSerializer, Group.class );
            }
            default:
            {
                return fromRequestBody( request, restSerializer, Repository.class );
            }
        }
    }

    public static <T> T fromRequestBody( final HttpServerRequest req, final JsonSerializer serializer, final Class<T> cls )
    {
        final JsonHandler handler = new JsonHandler();
        req.bodyHandler( handler );
        synchronized ( handler )
        {
            final long start = System.currentTimeMillis();
            while ( System.currentTimeMillis() - start < READ_TIMEOUT && handler.json == null )
            {
                try
                {
                    handler.wait( 100 );
                }
                catch ( final InterruptedException e )
                {
                    Thread.currentThread()
                          .interrupt();
                    return null;
                }
            }
        }

        return handler.json == null ? null : cls.cast( serializer.fromString( handler.json, cls ) );
    }

    private static final class JsonHandler
        implements Handler<Buffer>
    {
        private String json;

        @Override
        public synchronized void handle( final Buffer event )
        {
            json = event.getString( 0, event.length() );
            notifyAll();
        }
    }

}
