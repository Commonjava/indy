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
package org.commonjava.aprox.subsys.infinispan.inject;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Category;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.LoggerRepository;
import org.commonjava.aprox.subsys.infinispan.inject.fixture.TestKey;
import org.commonjava.aprox.subsys.infinispan.inject.fixture.TestTarget;
import org.commonjava.aprox.subsys.infinispan.inject.fixture.TestValue;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CacheProducerTest
{

    private Weld weld;

    private TestTarget target;

    private WeldContainer container;

    @BeforeClass
    public static void configure()
    {
        final Configurator log4jConfigurator = new Configurator()
        {
            public void doConfigure( final InputStream stream, final LoggerRepository repo )
            {
                final Level level = Level.DEBUG;
                final ConsoleAppender cAppender = new ConsoleAppender( new SimpleLayout() );
                cAppender.setThreshold( Level.ALL );

                repo.setThreshold( level );
                repo.getRootLogger()
                    .removeAllAppenders();
                repo.getRootLogger()
                    .setLevel( level );
                repo.getRootLogger()
                    .addAppender( cAppender );

                @SuppressWarnings( "unchecked" )
                final List<Logger> loggers = Collections.list( repo.getCurrentLoggers() );

                for ( final Logger logger : loggers )
                {
                    logger.setLevel( level );
                }

                @SuppressWarnings( "unchecked" )
                final List<Category> cats = Collections.list( repo.getCurrentCategories() );
                for ( final Category cat : cats )
                {
                    cat.setLevel( level );
                }
            }

            @Override
            public void doConfigure( final URL notUsed, final LoggerRepository repo )
            {
                doConfigure( (InputStream) null, repo );
            }
        };

        log4jConfigurator.doConfigure( null, LogManager.getLoggerRepository() );
    }

    @Before
    public void setup()
    {
        weld = new Weld();
        container = weld.initialize();

        target = container.instance()
                          .select( TestTarget.class )
                          .get();
    }

    @After
    public void teardown()
    {
        if ( weld != null && container != null )
        {
            weld.shutdown();
        }
    }

    @Test
    public void injectTypedCacheWithNamedAnnotation()
    {
        target.getCache()
              .put( new TestKey( "key" ), new TestValue( "value" ) );
        target.getDataCache()
              .put( "foo", "bar".getBytes() );
    }

}
