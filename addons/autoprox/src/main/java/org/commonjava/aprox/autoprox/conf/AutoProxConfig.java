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

    private List<FactoryMapping> factoryMappings;

    private boolean enabled;

    private String dataDir;

    public AutoProxConfig( final String dataDir, final boolean enabled, final List<FactoryMapping> factoryMappings )
    {
        this.dataDir = dataDir;
        this.factoryMappings = factoryMappings;
        this.enabled = enabled;
    }

    public String getDataDir()
    {
        return dataDir;
    }

    public void setDataDir( final String dataDir )
    {
        this.dataDir = dataDir;
    }

    public void setFactoryMappings( final List<FactoryMapping> mappings )
    {
        this.factoryMappings = mappings;
    }

    /**
     * Instead, store factory scripts in <aprox>/data/autoprox with names like:
     * <ul>
     *   <li>0001-foo-factory.groovy</li>
     *   <li>0002-bar-factory.groovy</li>
     * </ul>
     * 
     * Then, use the basedir configuration if you need to relocate these scripts elsewhere.
     */
    @Deprecated
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
