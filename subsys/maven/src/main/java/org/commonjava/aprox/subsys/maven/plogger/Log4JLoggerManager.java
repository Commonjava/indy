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
package org.commonjava.aprox.subsys.maven.plogger;

import java.util.WeakHashMap;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;

public class Log4JLoggerManager
    implements LoggerManager
{

    //    private final org.commonjava.util.logging.Logger logger = new org.commonjava.util.logging.Logger( getClass() );

    private final WeakHashMap<String, Logger> activeLoggers = new WeakHashMap<String, Logger>();

    private int masterLevel;

    @Override
    public int getActiveLoggerCount()
    {
        return activeLoggers.size();
    }

    @Override
    public Logger getLoggerForComponent( final String role )
    {
        Logger logger = activeLoggers.get( role );
        if ( logger == null )
        {
            logger = new Log4JLogger( role, masterLevel );
            activeLoggers.put( role, logger );
        }

        return logger;
    }

    @Override
    public Logger getLoggerForComponent( final String role, final String hint )
    {
        final String key = role + "#" + hint;
        Logger logger = activeLoggers.get( key );
        if ( logger == null )
        {
            logger = new Log4JLogger( key, masterLevel );
            activeLoggers.put( key, logger );
        }

        return logger;
    }

    @Override
    public int getThreshold()
    {
        return masterLevel;
    }

    @Override
    public void returnComponentLogger( final String role )
    {
        activeLoggers.remove( role );
    }

    @Override
    public void returnComponentLogger( final String role, final String hint )
    {
        activeLoggers.remove( role + "#" + hint );
    }

    @Override
    public void setThreshold( final int masterLevel )
    {
        this.masterLevel = masterLevel;
    }

    @Override
    public void setThresholds( final int level )
    {
        this.masterLevel = level;
        for ( final Logger logger : activeLoggers.values() )
        {
            logger.setThreshold( level );
        }
    }

}
