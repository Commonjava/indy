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
package org.commonjava.indy.subsys.kafka.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by ruhan on 12/20/18.
 */
public class LogbackFormatter
{

    private final Logger logger;

    private OutputStreamAppender<ILoggingEvent> appender;

    public LogbackFormatter( String loggerName )
    {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger = context.getLogger( loggerName );
        Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();
        while ( it.hasNext() )
        {
            Appender a = it.next();
            if ( a instanceof OutputStreamAppender )
            {
                appender = (OutputStreamAppender) a;
                return;
            }
        }
    }

    public LogbackFormatter( String loggerName, String appenderName )
    {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        logger = context.getLogger( loggerName );
        appender = (ConsoleAppender<ILoggingEvent>) logger.getAppender( appenderName );
    }

    public String format( String message ) throws IOException
    {
        if ( appender == null )
        {
            return message;
        }

        ILoggingEvent logEvent = new LoggingEvent( LogbackFormatter.class.getName(), logger, Level.INFO, message, null,
                                                   new Object[0] );
        byte[] encode = appender.getEncoder().encode( logEvent );
        return new String( encode );
    }
}
