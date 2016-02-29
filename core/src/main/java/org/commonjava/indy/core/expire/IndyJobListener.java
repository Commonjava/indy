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
