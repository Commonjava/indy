/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.junit.Test;

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
}
