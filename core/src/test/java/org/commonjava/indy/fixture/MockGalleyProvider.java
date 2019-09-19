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
package org.commonjava.indy.fixture;

import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;

//TODO: This provider is fully duplicated with org.commonjava.indy.test.fixture.core.MockGalleyProvider
//      in test/fixture-core for some dependency reasons in addons. Should be refactored in another
//      common test module in the future.

@ApplicationScoped
@Alternative
public class MockGalleyProvider
{
    @Produces
    public TransportManagerConfig getTransportManagerConfig()
    {
        return new TransportManagerConfig();
    }

    @Produces
    public MavenPluginDefaults getPluginDefaults()
    {
        return new MavenPluginDefaults()
        {
            @Override
            public String getDefaultGroupId( String artifactId )
            {
                return null;
            }

            @Override
            public String getDefaultVersion( String groupId, String artifactId )
            {
                return null;
            }

            @Override
            public String getDefaultVersion( ProjectRef ref )
            {
                return null;
            }
        };
    }

    @Produces
    public MavenPluginImplications getPluginImplications()
    {
        return pv -> null;
    }

    @Produces
    public GlobalHttpConfiguration getGlobalHttpConfiguration()
    {
        return null;
    }

}
