/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.metrics;

import io.undertow.servlet.api.DeploymentInfo;
import org.commonjava.indy.bind.jaxrs.IndyDeploymentProvider;
import org.commonjava.o11yphant.metrics.jaxrs.PrometheusDeploymentProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Application;

@ApplicationScoped
public class IndyPrometheusDeploymentProvider
        extends IndyDeploymentProvider
{
    @Inject
    private PrometheusDeploymentProvider prometheusDeploymentProvider;

    @Override
    public DeploymentInfo getDeploymentInfo( String contextRoot, Application application )
    {
        return prometheusDeploymentProvider.getDeploymentInfo( contextRoot );
    }
}
