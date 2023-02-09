/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.boot.jaxrs;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.MimeTyper;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.boot.PortFinder;
import org.commonjava.propulsor.deploy.DeployException;
import org.commonjava.propulsor.deploy.Deployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class IndyDeployer
        implements Deployer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Undertow server;

    public IndyDeployer()
    {
    }

    @Inject
    private IndyDeployment indyDeployment;

    @Inject
    private RestConfig restConfig;

    @Inject
    private MimeTyper mimeTyper;

    @Override
    public void stop()
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Override
    public void deploy( BootOptions bootOptions )
            throws DeployException
    {
        final DeploymentInfo di = indyDeployment.getDeployment( bootOptions.getContextPath() ).setContextPath( "/" );

        final DeploymentManager dm = Servlets.defaultContainer().addDeployment( di );

        Collection<String> list = Servlets.defaultContainer().listDeployments();
        logger.info( "List deployments: {}", list );

        dm.deploy();

        try
        {
            Integer port = bootOptions.getPort();
            if ( port < 1 )
            {
                logger.info( "Looking for open Undertow port..." );

                final AtomicReference<Exception> errorHolder = new AtomicReference<>();
                final AtomicReference<Integer> usingPort = new AtomicReference<>();

                server = PortFinder.findPortFor( 16, ( foundPort ) -> {
                    usingPort.set( foundPort );
                    try
                    {
                        return buildAndStartUndertow( dm, foundPort, bootOptions.getBind(), restConfig );
                    }
                    catch ( Exception e )
                    {
                        errorHolder.set( e );
                    }
                    return null;
                } );

                Exception e = errorHolder.get();
                if ( e != null )
                {
                    throw e;
                }
                bootOptions.setPort( usingPort.get() );
            }
            else
            {
                logger.info( "Start Undertow server, bind: {}, port: {}", bootOptions.getBind(), port );
                server = buildAndStartUndertow( dm, port, bootOptions.getBind(), restConfig );
            }

            logger.info( "Indy listening on {}:{}\n\n", bootOptions.getBind(), bootOptions.getPort() );
        }
        catch ( Exception e )
        {
            logger.error( "Deploy failed", e );
            throw new DeployException( "Deploy failed", e );
        }
    }

    private Undertow buildAndStartUndertow( DeploymentManager dm, Integer port, String bind, RestConfig restConfig )
            throws Exception
    {
        Undertow.Builder builder = Undertow.builder()
                                           .setServerOption( UndertowOptions.ENABLE_HTTP2, true )
                                           .setHandler( getGzipEncodeHandler( dm ) )
                                           .addHttpListener( port, bind );
        restConfig.configureBuilder( builder );
        Undertow t = builder.build();
        t.start();
        return t;
    }

    private static final Set<String> NO_NEED_GZIPPED_CONTENT = new HashSet<>();

    static
    {
        NO_NEED_GZIPPED_CONTENT.add( ApplicationContent.application_gzip );
        NO_NEED_GZIPPED_CONTENT.add( ApplicationContent.application_zip );
    }

    private EncodingHandler getGzipEncodeHandler( final DeploymentManager dm )
            throws ServletException
    {
        // FROM: https://stackoverflow.com/questions/28295752/compressing-undertow-server-responses#28329810
        final Predicate sizePredicate = Predicates.parse( "max-content-size[" + Long.toString( 5 * 1024 ) + "]" );

        // For firefox bug: https://bugzilla.mozilla.org/show_bug.cgi?id=610679
        final Predicate fileTypePredicate = v -> {
            final String path = v.getRequestPath();
            return !NO_NEED_GZIPPED_CONTENT.contains( mimeTyper.getContentType( path ).toLowerCase() );
        };

        final Predicate mixePredicate = v -> sizePredicate.resolve( v ) && fileTypePredicate.resolve( v );

        EncodingHandler eh = new EncodingHandler(
                new ContentEncodingRepository().addEncodingHandler( "gzip", new GzipEncodingProvider(), 50,
                                                                    mixePredicate ) ).setNext( dm.start() );
//                                               .addEncodingHandler( "deflate", new DeflateEncodingProvider(), 51,
//                                                                    sizePredicate ) ).setNext( dm.start() );
        return eh;
    }
}
