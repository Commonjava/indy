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
package org.commonjava.aprox.depgraph.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.subsys.flatfile.conf.DataFileManager;
import org.commonjava.web.config.ConfigurationException;

@ApplicationScoped
public class AproxDepgraphConfigProvider
    extends AbstractAproxFeatureConfig<AproxDepgraphConfig, AproxDepgraphConfig>
{
    @ApplicationScoped
    public static class AproxDepgraphConfigInfo
        extends AbstractAproxConfigInfo
    {
        public AproxDepgraphConfigInfo()
        {
            super( AproxDepgraphConfig.class );
        }
    }

    @Inject
    private AproxDepgraphConfigInfo info;

    @Inject
    private DataFileManager ffManager;

    public AproxDepgraphConfigProvider()
    {
        super( AproxDepgraphConfig.class );
    }

    @Produces
    @Production
    @Default
    public AproxDepgraphConfig getDepgraphConfig()
        throws ConfigurationException
    {
        return getConfig().setDirectories( ffManager.getDetachedDataBasedir(), ffManager.getDetachedWorkBasedir() );
    }

    @Override
    public AproxConfigInfo getInfo()
    {
        return info;
    }
}
