package org.commonjava.aprox.subsys.threads.inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class ExecutorProvider
{

    private final Map<String, ExecutorService> services = new HashMap<String, ExecutorService>();

    private final Logger logger = new Logger( getClass() );

    @PreDestroy
    public void shutdown()
    {
        for ( final Map.Entry<String, ExecutorService> entry : services.entrySet() )
        {
            final ExecutorService service = entry.getValue();

            service.shutdown();
            try
            {
                service.awaitTermination( 1000, TimeUnit.MILLISECONDS );

                if ( !service.isTerminated() )
                {
                    final List<Runnable> running = service.shutdownNow();
                    if ( !running.isEmpty() )
                    {
                        logger.warn( "%d tasks remain for executor: %s", running.size(), entry.getKey() );
                    }
                }
            }
            catch ( final InterruptedException e )
            {
            }
        }
    }

    @Produces
    public ExecutorService getExecutorService( final InjectionPoint ip )
    {
        final ExecutorConfig ec = ip.getAnnotated()
                                    .getAnnotation( ExecutorConfig.class );

        int threadCount = Runtime.getRuntime()
                                 .availableProcessors() * 2;

        // TODO: This may cause counter-intuitive sharing of thread pools for un-annotated injections...
        String name = "Unknown";

        if ( ec != null )
        {
            threadCount = ec.threads();
            name = ec.named();
        }

        return getService( name, threadCount );
    }

    private synchronized ExecutorService getService( final String name, final int threadCount )
    {
        ExecutorService service = services.get( name );
        if ( service == null )
        {
            service = Executors.newFixedThreadPool( threadCount, new ThreadFactory()
            {
                private int counter = 0;

                @Override
                public Thread newThread( final Runnable runnable )
                {
                    final Thread t = new Thread( runnable );
                    t.setName( name + "-" + counter++ );
                    t.setDaemon( true );
                    t.setPriority( 10 );

                    return t;
                }
            } );

            services.put( name, service );
        }

        return service;
    }

    //    @Produces
    //    public Executor getExecutor( final InjectionPoint ip )
    //    {
    //        return getExecutorService( ip );
    //    }

}
