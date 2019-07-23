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
package org.commonjava.indy.koji.ftest;

import org.commonjava.indy.koji.client.IndyKojiClientModule;
import org.commonjava.indy.koji.model.KojiRepairResult;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.commonjava.indy.koji.model.IndyKojiConstants.KOJI_ORIGIN_BINARY;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RepairKojiRepositoryTest
                extends ExternalKojiTest
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    final String path =
                    "org/dashbuilder/dashbuilder-all/0.4.0.Final-redhat-10/dashbuilder-all-0.4.0.Final-redhat-10.pom";

    final String binaryPath = "org/apache/apache/18/apache-18.pom";

    /**
     * For this test to pass, below settings are needed in koji.conf:
     *
     * tag.patterns.enabled=false
     * proxy.binary.builds=true
     */
    @Ignore
    @Test
    public void run() throws Exception
    {
        // 0. trigger koji repo creation
        contentDownloadTime( pseudoGroupName, path );
        contentDownloadTime( pseudoGroupName, binaryPath );

        List<RemoteRepository> repos = getKojiRemoteRepositories();

        RemoteRepository repository = null;
        RemoteRepository repositoryBinary = null;

        for ( RemoteRepository r : repos )
        {
            String name = r.getName();
            if ( name.startsWith( KOJI_ORIGIN_BINARY ) )
            {
                repositoryBinary = r;
            }
            else
            {
                repository = r;
            }
        }

        assertThat( repositoryBinary, notNullValue() );
        assertThat( repository, notNullValue() );

        // 1. repair remote
        IndyKojiClientModule module = client.module( IndyKojiClientModule.class );
        KojiRepairResult ret = module.repairVol( "maven", remote, repository.getName(), true );

        boolean succeeded = ret.succeeded();
        assertTrue( succeeded );

        printResults( ret );

        // 2. repair group
        String groupName = "brew-proxies";

        ret = module.repairVol( "maven", group, groupName, true );

        succeeded = ret.succeeded();
        assertTrue( succeeded );

        printResults( ret );

    }

    private void printResults( KojiRepairResult ret )
    {
        List<KojiRepairResult.RepairResult> results = ret.getResults();

        for ( KojiRepairResult.RepairResult result : results )
        {
            List<KojiRepairResult.PropertyChange> changes = result.getChanges();
            if ( changes != null )
            {
                for ( KojiRepairResult.PropertyChange change : changes )
                {
                    System.out.println( ">>> " + change );
                }
            }
        }
    }

}
