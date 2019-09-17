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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Copy of org.commonjava.indy.core.content.DefaultDownloadManagerTest#getTransferFromNotAllowedDeletionStore_DownloadOp_ThrowException()
 * that uses fully loaded CDI environment to propagate JEE events and handle clearing the CacheOnlyLocation when
 * a store is updated.
 *
 * Created by jdcasey on 10/26/17.
 */
public class HostedRepositoryDeleteNotAllowedWhenReadonly
    extends AbstractContentManagementTest
{
    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Test( expected = IOException.class )
    public void getTransferFromNotAllowedDeletionStore_DownloadOp_ThrowException() throws Exception
    {
        ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "Test setup" );
        HostedRepository hosted = new HostedRepository( MAVEN_PKG_KEY, "one" );

        hosted = client.stores().create( hosted, "Test setup", HostedRepository.class );

        String originalString = "This is a test";
        final String path = "/path/path";

        client.content().store( hosted.getKey(), path, new ByteArrayInputStream( originalString.getBytes() ) );

        hosted.setReadonly( true );
        client.stores().update( hosted, "make readonly" );

        client.content().delete( hosted.getKey(), path );
    }

}
