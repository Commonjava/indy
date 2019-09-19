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
package org.commonjava.indy.folo.ftest.report;

import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case test if the folo report is cached before the report is requested. It does this:
 * when: <br />
 * <ul>
 *      <li>create central store repo and store a pom</li>
 *      <li>generate the report for central and pom</li>
 *      <li>delete the pom but not the central repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the report can be still fetched with same before deletion</li>
 *     <li>all checksums should be generated and keep same</li>
 * </ul>
 */
public class CachedReportWhenDeleteTransferTest
        extends AbstractCacheReportTest
{
    @Test
    public void testDigestCache()
            throws Exception
    {
        doRealTest();
    }

    @Override
    protected void doDeletion( final StoreKey storeKey, final String path )
            throws Exception
    {
        client.content().delete( storeKey, path );
        Thread.sleep( 1000L );
    }
}
