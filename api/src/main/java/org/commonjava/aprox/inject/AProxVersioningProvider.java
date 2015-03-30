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
package org.commonjava.aprox.inject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.stats.AProxVersioning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer class that reads a properties file off the classpath containing version info for AProx, and assembles an instance of {@link AProxVersioning},
 * which this component then provides for injecting into other components.
 */
@Singleton
public class AProxVersioningProvider
{

    private static final String APROX_VERSIONING_PROPERTIES = "aprox-version.properties";

    private final AProxVersioning versioning;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public AProxVersioningProvider()
    {
        final Properties props = new Properties();
        InputStream is = null;
        try
        {
            is = Thread.currentThread()
                       .getContextClassLoader()
                       .getResourceAsStream( APROX_VERSIONING_PROPERTIES );
            if ( is != null )
            {
                props.load( is );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read AProx versioning information from classpath resource: "
                + APROX_VERSIONING_PROPERTIES, e );
        }
        finally
        {
            IOUtils.closeQuietly( is );
        }

        versioning =
            new AProxVersioning( props.getProperty( "version", "unknown" ), props.getProperty( "builder", "unknown" ),
                                 props.getProperty( "commit.id", "unknown" ),
                                 props.getProperty( "timestamp", "unknown" ) );
    }

    @Produces
    @Default
    public AProxVersioning getVersioningInstance()
    {
        return versioning;
    }

}
