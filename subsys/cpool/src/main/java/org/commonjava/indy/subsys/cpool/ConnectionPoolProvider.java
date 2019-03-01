package org.commonjava.indy.subsys.cpool;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.sun.jndi.rmi.registry.RegistryContextFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.boot.PortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        properties.setProperty( CPInitialContextFactory.FACTORY_SYSPROP, CPInitialContextFactory.class.getName() );
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
            HikariConfig cfg = new HikariConfig();
            cfg.setPoolName( poolInfo.getName() );
            cfg.setJdbcUrl( poolInfo.getUrl() );

            if ( poolInfo.getUser() != null )
            {
                cfg.setUsername( poolInfo.getUser() );
            }

            if ( poolInfo.getPassword() != null )
            {
                cfg.setPassword( poolInfo.getPassword() );
            }

            if ( poolInfo.getDataSourceClassname() != null )
            {
                cfg.setDataSourceClassName( poolInfo.getDataSourceClassname() );
            }

            if ( poolInfo.getDriverClassname() != null )
            {
                cfg.setDriverClassName( poolInfo.getDriverClassname() );
            }

            if ( poolInfo.isUseMetrics() )
            {
                cfg.setMetricRegistry( metricRegistry );
            }

            if ( poolInfo.isUseHealthChecks() )
            {
                cfg.setHealthCheckRegistry( healthCheckRegistry );
            }

            Properties props = new Properties();
            poolInfo.getProperties().forEach( props::setProperty );
            if ( !props.isEmpty())
            {
                cfg.setDataSourceProperties( props );
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
