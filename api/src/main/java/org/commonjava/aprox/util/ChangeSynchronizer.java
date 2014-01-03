/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.util;

import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class ChangeSynchronizer
{

    private final Logger logger = new Logger( getClass() );

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
        logger.debug( "Waiting for %d events to occur...%d have already happened.", count, changed );
        final long start = System.currentTimeMillis();
        double runningTotal = 0;

        while ( changed < count )
        {
            runningTotal = ( System.currentTimeMillis() - start );
            logger.debug( "Waited (%s ms)...", runningTotal );

            if ( runningTotal > ( totalMillis ) )
            {
                logger.debug( "Wait (%s ms) expired.", totalMillis );
                break;
            }

            try
            {
                logger.debug( "Waiting (%s ms) for changes.", pollMillis );
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
