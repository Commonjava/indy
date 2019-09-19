/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.cpool;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.commonjava.indy.action.IndyLifecycleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Map;
import java.util.Properties;

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;

@ApplicationScoped
public class ConnectionPoolProvider
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

//    private Registry registry;
//    private int registryPort;

    @Inject
    private ConnectionPoolConfig config;

    @Inject
    private MetricRegistry metricRegistry;

    @Inject
    private HealthCheckRegistry healthCheckRegistry;

    public void init()
            throws IndyLifecycleException
    {
        logger.info( "Starting connection pool binding..." );
//        if ( registry == null )
//        {
//            AtomicReference<IndyLifecycleException> err = new AtomicReference<>();
//            registryPort = PortFinder.findPortFor( 16, (port)->{
//                try
//                {
//                    registry = LocateRegistry.createRegistry( port );
//                    logger.info( "Started RMI Registry on port: {} (for JNDI connection-pool bindings)", port );
//                    return port;
//                }
//                catch ( RemoteException e )
//                {
//                    err.set( new IndyLifecycleException( "Failed to start RMI / JNDI registry on port 1099",
//                                                         e ) );
//                }
//
//                return -1;
//            } );
//
//            if ( err.get() != null )
//            {
//                throw err.get();
//            }
//        }
//
//        logger.info( "RMI / JNDI Registry running on port: {}", registryPort );

        Properties properties = System.getProperties();
        properties.setProperty( INITIAL_CONTEXT_FACTORY, CPInitialContextFactory.class.getName() );
//        properties.setProperty( "java.naming.provider.url", "rmi://127.0.0.1:" + registryPort );
        System.setProperties( properties );

        InitialContext ctx;
        try
        {
            ctx = new InitialContext();
        }
        catch ( NamingException e )
        {
            throw new IndyLifecycleException( "Failed to create JNDI InitialContext for binding datasources", e );
        }

        Map<String, ConnectionPoolInfo> poolConfigs = config.getPools();
        logger.info( "Creating bindings for {} pools from config: {}", poolConfigs.size(), config );
        for ( ConnectionPoolInfo poolInfo : poolConfigs.values() )
        {
            HikariConfig cfg = new HikariConfig( poolInfo.getProperties() );
            cfg.setPoolName( poolInfo.getName() );

            if ( poolInfo.isUseMetrics() )
            {
                cfg.setMetricRegistry( metricRegistry );
            }

            if ( poolInfo.isUseHealthChecks() )
            {
                cfg.setHealthCheckRegistry( healthCheckRegistry );
            }

            HikariDataSource ds = new HikariDataSource( cfg );
            try
            {
                String jndiName = "java:/comp/env/jdbc/" + poolInfo.getName();
                logger.info( "Binding datasource: {}", jndiName );
                ctx.rebind( jndiName, ds );
            }
            catch ( NamingException e )
            {
                throw new IndyLifecycleException( "Failed to bind datasource: " + poolInfo.getName(), e );
            }
        }
    }

}
