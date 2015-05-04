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
