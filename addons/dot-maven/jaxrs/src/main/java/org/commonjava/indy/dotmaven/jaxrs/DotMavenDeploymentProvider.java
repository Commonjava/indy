/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.dotmaven.jaxrs;

import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.ws.rs.core.Application;

public class DotMavenDeploymentProvider
    extends IndyDeploymentProvider
{

    @Inject
    DotMavenServlet servlet;

    @Override
    public DeploymentInfo getDeploymentInfo( String contextRoot, Application application )
    {
        final ServletInfo servletInfo = Servlets.servlet( "DotMaven", DotMavenServlet.class )
                                                .setAsyncSupported( true )
                                                .setLoadOnStartup( 3 )
                                                .addMapping( "/mavdav*" )
                                                .addMapping( "/mavdav/*" );

        servletInfo.setInstanceFactory( new ImmediateInstanceFactory<Servlet>( servlet ) );

        return new DeploymentInfo().addServlet( servletInfo );
    }
}
