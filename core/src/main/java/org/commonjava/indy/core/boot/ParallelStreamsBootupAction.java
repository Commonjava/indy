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
