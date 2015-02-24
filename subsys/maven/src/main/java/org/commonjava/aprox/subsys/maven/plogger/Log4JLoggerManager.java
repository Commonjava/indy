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
            logger = new Log4JLogger( role );
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
            logger = new Log4JLogger( key );
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

    @Override
    public void setThreshold( final String role, final int threshold )
    {
        getLoggerForComponent( role ).setThreshold( threshold );
    }

    @Override
    public void setThreshold( final String role, final String roleHint, final int threshold )
    {
        getLoggerForComponent( role, roleHint ).setThreshold( threshold );
    }

    @Override
    public int getThreshold( final String role )
    {
        return getLoggerForComponent( role ).getThreshold();
    }

    @Override
    public int getThreshold( final String role, final String roleHint )
    {
        return getLoggerForComponent( role, roleHint ).getThreshold();
    }

}
