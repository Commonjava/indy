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
package org.commonjava.aprox.autoprox.conf;

import java.util.List;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

@Alternative
@Named( "dont-inject-directly" )
public class AutoProxConfig
{

    private final List<FactoryMapping> factoryMappings;

    private boolean enabled;

    public AutoProxConfig( final List<FactoryMapping> factoryMappings, final boolean enabled )
    {
        this.factoryMappings = factoryMappings;
        this.enabled = enabled;
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

}
