/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.depgraph.ftest;

import com.fasterxml.jackson.databind.Module;
import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.boot.AproxBootException;
import org.commonjava.aprox.boot.BootStatus;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.module.AproxRawObjectMapperModule;
import org.commonjava.aprox.depgraph.client.DepgraphAproxClientModule;
import org.commonjava.aprox.depgraph.impl.ClientCartographer;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.test.fixture.core.CoreServerFixture;
import org.commonjava.cartographer.Cartographer;
import org.commonjava.maven.cartographer.ftest.CartoTCKDriver;
import org.commonjava.test.http.StreamServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class AproxCartoTCKDriver
    implements CartoTCKDriver
{
    protected Aprox client;

    protected CoreServerFixture fixture;

    private Set<StreamServer> servers = new HashSet<>();

    private ClientCartographer carto;

    public Cartographer start( TemporaryFolder temp )
            throws Exception
    {
        fixture = newServerFixture( temp );
        fixture.start();

        if ( !fixture.isStarted() )
        {
            final BootStatus status = fixture.getBootStatus();
            throw new IllegalStateException( "server fixture failed to boot.", status.getError() );
        }

        client =
                new Aprox( fixture.getUrl(), new DepgraphAproxClientModule(), new AproxRawObjectMapperModule() ).connect();

        carto = new ClientCartographer( client );

        return carto;
    }

    public void stop()
    {
        closeQuietly( fixture );

        if ( carto != null )
        {
            carto.close();
        }

        closeQuietly( client );

        servers.forEach( org.commonjava.test.http.StreamServer::stop );
    }

    @Override
    public void createRepoAlias( String alias, String repoResource )
            throws Exception
    {
        String url = repoResource;
        if ( repoResource.startsWith("file:") || repoResource.startsWith("jar:") )
        {
            // setup test http server to serve content, then:
            String path = new URI(repoResource).getPath();
            File f = new File( path );

            StreamServer server = new StreamServer( f ).start();
            servers.add( server );

            url = server.getBaseUri();
        }
        // else, it's already a remote repo (for a test server setup by the test itself)

        client.stores().create( new RemoteRepository(alias, url), "Adding test repo: " + alias, RemoteRepository.class );
        carto.setSourceAlias( alias, new StoreKey( StoreType.remote, alias ).toString() );
    }

    protected CoreServerFixture newServerFixture( TemporaryFolder temp )
        throws AproxBootException, IOException
    {
        return new CoreServerFixture( temp );
    }

}
