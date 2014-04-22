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

import org.apache.log4j.Level;
import org.codehaus.plexus.logging.Logger;

public class Log4JLogger
    implements Logger
{

    private final org.apache.log4j.Logger logger;

    public Log4JLogger( final String key )
    {
        logger = org.apache.log4j.Logger.getLogger( key );
    }

    @Override
    public void debug( final String message )
    {
        logger.debug( message );
    }

    @Override
    public void debug( final String message, final Throwable error )
    {
        logger.debug( message, error );
    }

    @Override
    public void error( final String message )
    {
        logger.error( message );
    }

    @Override
    public void error( final String message, final Throwable error )
    {
        logger.error( message, error );
    }

    @Override
    public void fatalError( final String message )
    {
        logger.fatal( message );
    }

    @Override
    public void fatalError( final String message, final Throwable error )
    {
        logger.fatal( message, error );
    }

    @Override
    public Logger getChildLogger( final String name )
    {
        return new Log4JLogger( logger.getName() + "." + name );
    }

    @Override
    public String getName()
    {
        return logger.getName();
    }

    @Override
    public int getThreshold()
    {
        return logger.getLevel()
                     .toInt();
    }

    @Override
    public void info( final String message )
    {
        logger.info( message );
    }

    @Override
    public void info( final String message, final Throwable error )
    {
        logger.info( message, error );
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isEnabledFor( Level.DEBUG );
    }

    @Override
    public boolean isErrorEnabled()
    {
        return logger.isEnabledFor( Level.ERROR );
    }

    @Override
    public boolean isFatalErrorEnabled()
    {
        return logger.isEnabledFor( Level.FATAL );
    }

    @Override
    public boolean isInfoEnabled()
    {
        return logger.isEnabledFor( Level.INFO );
    }

    @Override
    public boolean isWarnEnabled()
    {
        return logger.isEnabledFor( Level.WARN );
    }

    @Override
    public void setThreshold( final int level )
    {
    }

    @Override
    public void warn( final String message )
    {
        logger.warn( message );
    }

    @Override
    public void warn( final String message, final Throwable error )
    {
        logger.warn( message, error );
    }

}
