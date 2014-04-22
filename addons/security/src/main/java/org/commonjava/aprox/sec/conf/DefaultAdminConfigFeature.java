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
package org.commonjava.aprox.sec.conf;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.inject.Production;
import org.commonjava.badgr.conf.AdminConfiguration;
import org.commonjava.badgr.conf.DefaultAdminConfiguration;
import org.commonjava.web.config.ConfigurationException;

@javax.enterprise.context.ApplicationScoped
public class DefaultAdminConfigFeature
    extends AbstractAproxFeatureConfig<AdminConfiguration, DefaultAdminConfiguration>
{
    @Inject
    private DefaultAdminConfigInfo info;

    public DefaultAdminConfigFeature()
    {
        super( DefaultAdminConfiguration.class );
    }

    @Produces
    @Production
    @Default
    public AdminConfiguration getCacheConfig()
        throws ConfigurationException
    {
        return getConfig();
    }

    @Override
    public AproxConfigInfo getInfo()
    {
        return info;
    }

    @javax.enterprise.context.ApplicationScoped
    public static class DefaultAdminConfigInfo
        extends AbstractAproxConfigInfo
    {
        public DefaultAdminConfigInfo()
        {
            super( DefaultAdminConfiguration.class );
        }
    }

}
