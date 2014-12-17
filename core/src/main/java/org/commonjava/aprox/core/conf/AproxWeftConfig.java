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
package org.commonjava.aprox.core.conf;

import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.aprox.inject.Production;
import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;
import org.commonjava.web.config.ConfigurationException;

@ApplicationScoped
@Named( AproxWeftConfig.SECTION_NAME )
public class AproxWeftConfig
    extends AbstractAproxMapConfig
{

    public static final String SECTION_NAME = "threadpools";

    public static final String DEFAULT_THREADS = "defaultThreads";

    public static final String DEFAULT_PRIORITY = "defaultPriority";

    public static final String THREADS_SUFFIX = ".threads";

    public static final String PRIORITY_SUFFIX = ".priority";

    private final DefaultWeftConfig weftConfig = new DefaultWeftConfig();

    private final String numericPattern = "[0-9]+";

    public AproxWeftConfig()
    {
        super( SECTION_NAME );
    }

    @Override
    public void parameter( final String name, final String value )
        throws ConfigurationException
    {

        if ( !value.matches( numericPattern ) )
        {
            throw new ConfigurationException( "Invalid value: '{}' for parameter: '{}'. Only numeric values are accepted for section: '{}'.", value,
                                              name, SECTION_NAME );
        }

        final int v = Integer.parseInt( value );

        if ( DEFAULT_THREADS.equals( name ) )
        {
            weftConfig.configureDefaultThreads( v );
        }
        else if ( DEFAULT_PRIORITY.equals( name ) )
        {
            weftConfig.configureDefaultPriority( v );
        }
        else
        {
            final int lastIdx = name.lastIndexOf( '.' );
            if ( lastIdx > -1 && name.length() > lastIdx )
            {
                final String pool = name.substring( 0, lastIdx );
                final String suffix = name.substring( lastIdx );

                if ( THREADS_SUFFIX.equals( suffix ) )
                {
                    weftConfig.configureThreads( pool, v );
                }
                else if ( PRIORITY_SUFFIX.equals( suffix ) )
                {
                    weftConfig.configurePriority( pool, v );
                }
            }
        }
    }

    @Produces
    @Production
    @Default
    public DefaultWeftConfig getWeftConfig()
    {
        return weftConfig;
    }

    @Override
    public void sectionStarted( final String name )
        throws ConfigurationException
    {
        // NOP; just block map init in the underlying implementation.
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/threadpools.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-threadpools.conf" );
    }

}
