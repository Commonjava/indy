/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.repo.proxy.servlet;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import jnr.ffi.annotations.In;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.indy.repo.proxy.conf.RepoProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.ws.rs.core.Application;

import static org.commonjava.indy.repo.proxy.servlet.RepositoryProxyFilter.FILTER_NAME;

@ApplicationScoped
public class RepoProxyDeploymentProvider
        extends IndyDeploymentProvider
{
    @Inject
    private RepositoryProxyFilter filter;

    @Inject
    private RepoProxyConfig config;

    @Override
    public DeploymentInfo getDeploymentInfo( String contextRoot, Application application )
    {
        if ( !config.isEnabled() )
        {
            final Logger logger = LoggerFactory.getLogger( getClass() );
            logger.info( "Repository Proxy: addon not enabled, will not register the filter of the addon." );
            return null;
        }
        final FilterInfo filterInfo = Servlets.filter( FILTER_NAME, RepositoryProxyFilter.class );

        filterInfo.setInstanceFactory( new ImmediateInstanceFactory<Filter>( filter ) );
        DeploymentInfo deploymentInfo = new DeploymentInfo().addFilter( filterInfo );
        for ( final String urlPattern : config.getApiPatterns() )
        {
            deploymentInfo =
                    deploymentInfo.addFilterUrlMapping( filterInfo.getName(), urlPattern, DispatcherType.REQUEST );
        }

        return deploymentInfo;
    }
}
