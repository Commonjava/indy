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
package org.commonjava.indy.subsys.http.util;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.indy.subsys.http.conf.IndyHttpConfig;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.commonjava.indy.subsys.http.conf.IndyHttpConfig.DEFAULT_SITE;
import static org.commonjava.util.jhttpc.auth.AttributePasswordManager.PASSWORD_PREFIX;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 2/15/16.
 */
public class IndySiteConfigLookupTest
{
    @Test
    public void checkServerCertPemIsConfigured()
            throws IndyDataException
    {
        RemoteRepository remote = new RemoteRepository( MAVEN_PKG_KEY, "test", "http://test.com/repo" );
        remote.setServerCertPem( "AAAAFFFFFSDADFADSFASDFASDFASDFASDFASDFsa" );
        remote.setServerTrustPolicy( "self-signed" );

        MemoryStoreDataManager storeData = new MemoryStoreDataManager(true);
        storeData.storeArtifactStore( remote, new ChangeSummary( ChangeSummary.SYSTEM_USER, "This is a test" ), false,
                                      false, new EventMetadata() );

        IndySiteConfigLookup lookup = new IndySiteConfigLookup( storeData );
        SiteConfig siteConfig = lookup.lookup( "remote:test" );

        assertThat( siteConfig.getServerCertPem(), equalTo( remote.getServerCertPem() ) );
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void checkHttpConfigLoad() throws Exception
    {
        File pemFile = testFolder.newFile( "keycloak.pem");

        final String _URI = "http://site.com";
        final String _PROXY_HOST = "http://proxy.com";
        final String _PROXY_PORT = "8001";
        final String _PEM = "AAAAFFFFFSDADFADSFASDFASDFASDFASDFASDF";
        final String _KEY_PASSWORD = "testme";
        final String _KEYCLOAK_URI = "http://keycloak.com";
        final String _KEYCLOAK_PEM_PATH = pemFile.getAbsolutePath();

        IOUtils.write( _PEM, new FileOutputStream( pemFile ) );

        final class TestIndyHttpConfig extends IndyHttpConfig
        {
            @Override
            public Map<String, String> getConfiguration() {
                Map<String, String> parameters = new HashMap<>();
                parameters.put( "uri", _URI );
                parameters.put( "proxy.host", _PROXY_HOST );
                parameters.put( "proxy.port", _PROXY_PORT );
                parameters.put( "key.cert.pem", _PEM );
                parameters.put( "key.password", _KEY_PASSWORD );
                parameters.put( "keycloak_yourdomain_com.uri", _KEYCLOAK_URI );
                parameters.put( "keycloak_yourdomain_com.key.cert.pem.path", _KEYCLOAK_PEM_PATH );
                return parameters;
            }
        }

        IndyHttpConfig config = new TestIndyHttpConfig();
        config.sectionComplete( "http" );

        IndySiteConfigLookup lookup = new IndySiteConfigLookup( null, config);

        SiteConfig siteConfig = lookup.lookup( DEFAULT_SITE );
        assertThat( siteConfig.getUri(), equalTo( _URI ) );
        assertThat( siteConfig.getProxyHost(), equalTo( _PROXY_HOST ) );
        assertThat( siteConfig.getProxyPort(), equalTo( Integer.parseInt( _PROXY_PORT ) ) );
        assertThat( siteConfig.getKeyCertPem(), equalTo( _PEM ) );
        assertThat( siteConfig.getAttribute( PASSWORD_PREFIX + PasswordType.KEY.name() ), equalTo( _KEY_PASSWORD ) );

        SiteConfig keycloakConfig = lookup.lookup( "keycloak.yourdomain.com" );
        assertThat( keycloakConfig.getUri(), equalTo( _KEYCLOAK_URI ) );
        assertThat( keycloakConfig.getKeyCertPem(), equalTo( _PEM ) );
    }

}
