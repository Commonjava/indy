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

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.web.config.ConfigurationException;

@ApplicationScoped
@Named( AproxSchedulerConfig.SECTION_NAME )
public class AproxSchedulerConfig
    extends AbstractAproxMapConfig
{

    public static final String SECTION_NAME = "scheduler";

    public AproxSchedulerConfig()
    {
        super( SECTION_NAME );
    }

    public AproxSchedulerConfig( final Properties props )
        throws ConfigurationException
    {
        super( SECTION_NAME );
        this.sectionStarted( SECTION_NAME );
        for ( final String key : props.stringPropertyNames() )
        {
            this.parameter( key, props.getProperty( key ) );
        }
    }

}
