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
package org.commonjava.indy.diag.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@ApplicationScoped
public class LoggerManager
{
    private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger( this.getClass() );

    private final static List VALID_LEVELS = Arrays.asList( "TRACE", "DEBUG", "INFO", "WARN", "ERROR" );

    private List<String> getAppenders( Logger logger )
    {
        final Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();
        final List<String> appenderNames = new ArrayList<>();
        while ( it.hasNext() )
        {
            Appender<ILoggingEvent> appender = it.next();
            appenderNames.add( appender.getName() );
        }
        return appenderNames;
    }

    public List<LoggerDTO> getConfiguredLoggers()
    {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final List<LoggerDTO> loggers = new ArrayList<>();
        for ( Logger log : lc.getLoggerList() )
        {
            final List<String> appenderNames = getAppenders( log );
            if ( log.getLevel() != null || !appenderNames.isEmpty() )
            {
                final LoggerDTO dto = new LoggerDTO();
                dto.setName( log.getName() );
                dto.setLevel( log.getLevel().toString() );
                dto.setAdditive( log.isAdditive() );
                dto.setAppenders( appenderNames );
                loggers.add( dto );
            }
        }

        return loggers;
    }

    public LoggerDTO getLogger( final String name )
    {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger log = lc.getLogger( name );
        if ( log != null )
        {
            final LoggerDTO dto = new LoggerDTO();
            dto.setName( log.getName() );
            dto.setLevel( log.getLevel() != null ? log.getLevel().toString() : log.getEffectiveLevel().toString() );
            dto.setAdditive( log.isAdditive() );
            dto.setAppenders( getAppenders( log ) );
            return dto;
        }
        return null;
    }

    public LoggerDTO getConfiguredLogger( final String name )
    {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger log = lc.getLogger( name );
        if ( log.getLevel() != null || !getAppenders( log ).isEmpty() )
        {
            final LoggerDTO dto = new LoggerDTO();
            dto.setName( log.getName() );
            dto.setLevel( log.getLevel().toString() );
            dto.setAdditive( log.isAdditive() );
            dto.setAppenders( getAppenders( log ) );
            return dto;
        }
        return null;
    }

    public LoggerDTO changeConfiguredLogLevel( String name, String logLevel )
    {
        if ( !VALID_LEVELS.contains( logLevel ) )
        {
            return null;
        }
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        final Logger log = loggerContext.getLogger( name );
        final String originalLevel =
                log.getLevel() != null ? log.getLevel().toString() : log.getEffectiveLevel().toString();

        log.setLevel( Level.toLevel( logLevel ) );

        final LoggerDTO dto = new LoggerDTO();
        dto.setName( log.getName() );
        dto.setLevel( log.getLevel() != null ? log.getLevel().toString() : log.getEffectiveLevel().toString() );
        dto.setOriginalLevel( originalLevel );
        dto.setAdditive( log.isAdditive() );
        dto.setAppenders( getAppenders( log ) );

        return dto;
    }

}
