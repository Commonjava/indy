package org.commonjava.indy.core.boot;

import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.conf.IndyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Created by ruhan on 11/22/18.
 */
@ApplicationScoped
public class ParallelStreamsBootupAction
                implements BootupAction
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String FORKJOINPOOL_COMMON_PARALLELISM = "java.util.concurrent.ForkJoinPool.common.parallelism";

    @Inject
    private IndyConfiguration config;

    @Override
    public String getId()
    {
        return "parallel streams common pool";
    }

    @Override
    public void init() throws IndyLifecycleException
    {
        Integer commonPoolSize = null;
        if ( System.getProperty( FORKJOINPOOL_COMMON_PARALLELISM ) != null )
        {
            commonPoolSize = Integer.parseInt( System.getProperty( FORKJOINPOOL_COMMON_PARALLELISM ) );
        }
        else if ( config.getForkJoinPoolCommonParallelism() > 0 )
        {
            commonPoolSize = config.getForkJoinPoolCommonParallelism();
            System.setProperty( FORKJOINPOOL_COMMON_PARALLELISM, commonPoolSize.toString() );
        }

        if ( commonPoolSize != null )
        {
            logger.info( "Set {}={}", FORKJOINPOOL_COMMON_PARALLELISM, commonPoolSize );
        }
    }

    @Override
    public int getBootPriority()
    {
        return 99;
    }
}
