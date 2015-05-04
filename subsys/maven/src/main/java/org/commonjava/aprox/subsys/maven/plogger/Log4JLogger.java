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
