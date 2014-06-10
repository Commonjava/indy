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
package org.commonjava.aprox.autoprox.model;

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.autoprox.conf.FactoryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Alternative
@Named( "dont-inject-directly" )
public class AutoProxCatalog
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final List<FactoryMapping> factoryMappings;

    private boolean enabled;

    public AutoProxCatalog( final boolean enabled, final List<FactoryMapping> factoryMappings )
    {
        this.factoryMappings = factoryMappings;
        this.enabled = enabled;
    }

    public AutoProxCatalog( final boolean enabled )
    {
        this.enabled = enabled;
        this.factoryMappings = Collections.emptyList();
    }

    public List<FactoryMapping> getFactoryMappings()
    {
        return factoryMappings;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public AutoProxFactory getFactory( final String name )
    {
        for ( final FactoryMapping mapping : getFactoryMappings() )
        {
            if ( mapping.matchesName( name ) )
            {
                final AutoProxFactory factory = mapping.getFactory();

                logger.info( "Using factory {} (script: {}) for new store: '{}'", factory.getClass()
                                                                                         .getSimpleName(),
                             mapping.getScriptName(), name );

                return factory;
            }
        }

        logger.info( "No AutoProx factory found for: '{}'", name );

        return null;
    }

}
