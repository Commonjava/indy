package org.commonjava.aprox.core.expire;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpirationJob
    implements Job
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void execute( final JobExecutionContext context )
        throws JobExecutionException
    {
        logger.info( "Executing dummy job for AProx ScheduleManager. Actual changes flow through job/scheduler listeners." );
    }

}
