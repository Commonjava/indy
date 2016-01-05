package org.commonjava.indy.core.expire;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdcasey on 1/4/16.
 */
public class IndyJobListener
        implements JobListener
{
    @Override
    public String getName()
    {
        return "Indy Jobs";
    }

    @Override
    public void jobToBeExecuted( JobExecutionContext context )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Preparing execution of: {}", context.getJobDetail().getKey() );
    }

    @Override
    public void jobExecutionVetoed( JobExecutionContext context )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "VETOED execution of: {}", context.getJobDetail().getKey() );
    }

    @Override
    public void jobWasExecuted( JobExecutionContext context, JobExecutionException jobException )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Completed execution of: {}", context.getJobDetail().getKey() );
    }
}
