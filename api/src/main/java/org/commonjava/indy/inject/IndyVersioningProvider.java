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
package org.commonjava.indy.inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.stats.IndyVersioning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer class that reads a properties file off the classpath containing version info for Indy, and assembles an instance of {@link IndyVersioning},
 * which this component then provides for injecting into other components.
 */
@Singleton
public class IndyVersioningProvider
{

    private static final String INDY_VERSIONING_PROPERTIES = "indy-version.properties";

    private final IndyVersioning versioning;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public IndyVersioningProvider()
    {
        final Properties props = new Properties();
        InputStream is = null;
        try
        {
            is = Thread.currentThread()
                       .getContextClassLoader()
                       .getResourceAsStream( INDY_VERSIONING_PROPERTIES );
            if ( is != null )
            {
                props.load( is );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read Indy versioning information from classpath resource: "
                + INDY_VERSIONING_PROPERTIES, e );
        }
        finally
        {
            IOUtils.closeQuietly( is );
        }

        versioning =
            new IndyVersioning( props.getProperty( "version", "unknown" ), props.getProperty( "builder", "unknown" ),
                                 props.getProperty( "commit.id", "unknown" ),
                                 props.getProperty( "timestamp", "unknown" ), props.getProperty( "api-version",
                                                                                                 "unknown" ) );
    }

    @Produces
    @Default
    public IndyVersioning getVersioningInstance()
    {
        return versioning;
    }

}
