/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.util;

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
