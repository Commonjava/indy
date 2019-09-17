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
package org.commonjava.indy.ftest.core.lifecycle;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.subsys.http.util.IndySiteConfigLookup;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.junit.Ignore;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;

import static org.commonjava.indy.subsys.http.conf.IndyHttpConfig.DEFAULT_SITE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class HttpSiteConfigTest
        extends AbstractIndyFunctionalTest
{
    /**
     * TODO: How to get the injected siteConfigLookup? It is null...
     * We would have to change AbstractIndyFunctionalTest to use a WeldBootInterface instead of just BootInterface.
     * Then, we would have to expose / wrap WeldBootInterface.getContainer() and allow lookup of injected components.
     * Not impossible, but not trivial either.
     *
     * I use another unit test to test the config loading in http/test.
     *
     * The test here is only useful to verify http.conf could be loaded via normal Indy startup (by looking at the log),
     * and not worth running it every time. I make it ignored from normal test set.
     */
    @Inject
    private IndySiteConfigLookup siteConfigLookup;

    @Ignore
    @Test
    public void run()
        throws Exception
    {
        String siteId = DEFAULT_SITE;

        SiteConfig conf = siteConfigLookup.lookup( siteId );

        assertThat( conf.getKeyCertPem(), equalTo( PEM_CONTENT ) );
    }

    final static String PEM_CONTENT = "AAAAFFFFFSDADFADSFASDFASDFASDFASDFASDF";

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/http.conf", "[http]\n"
                        + "enabled=true\n"
                        + "key.cert.pem=" + PEM_CONTENT + "\n"
                        + "max.connections=20\n"
                        + "keycloak_yourdomain_com.key.cert.pem.path=" + fixture.getBootOptions().getHomeDir() + "/etc/indy/keycloak.pem\n"
                        + "keycloak_yourdomain_com.request.timeout.seconds=10" );

        writeConfigFile( "keycloak.pem", PEM_CONTENT );
    }
}
