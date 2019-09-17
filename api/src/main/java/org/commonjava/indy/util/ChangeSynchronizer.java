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
package org.commonjava.indy.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class ChangeSynchronizer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private int changed = 0;

    public synchronized void setChanged()
    {
        setChanged( 1 );
    }

    public synchronized void setChanged( final int changed )
    {
        this.changed += changed;
        logger.debug( "setting changed = " + changed );
        notifyAll();
    }

    public synchronized void addChanged()
    {
        this.changed++;
        logger.debug( "Adding change: " + this.changed );
        notifyAll();
    }

    public void resetChanged()
    {
        logger.debug( "RESET" );
        changed = 0;
    }

    public synchronized int waitForChange( final int count, final long totalMillis, final long pollMillis )
    {
        logger.debug( "Waiting for {} events to occur...{} have already happened.", count, changed );
        final long start = System.currentTimeMillis();
        double runningTotal = 0;

        while ( changed < count )
        {
            runningTotal = ( System.currentTimeMillis() - start );
            logger.debug( "Waited ({} ms)...", runningTotal );

            if ( runningTotal > ( totalMillis ) )
            {
                logger.debug( "Wait ({} ms) expired.", totalMillis );
                break;
            }

            try
            {
                logger.debug( "Waiting ({} ms) for changes.", pollMillis );
                wait( pollMillis );
            }
            catch ( final InterruptedException e )
            {
                break;
            }
        }

        if ( changed >= count )
        {
            logger.debug( "Setting changed state to false." );
            resetChanged();
        }

        logger.debug( "waitForChange() exiting." );

        return changed;
    }

}
