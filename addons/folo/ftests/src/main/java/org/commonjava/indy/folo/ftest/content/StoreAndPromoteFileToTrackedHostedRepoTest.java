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
package org.commonjava.indy.folo.ftest.content;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.dto.TrackedContentEntryDTO;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StoreAndPromoteFileToTrackedHostedRepoTest
                extends AbstractFoloContentManagementTest
{
    final String path = "/path/to/foo.class";

    final byte[] bytes = ( "This is a test: " + System.nanoTime() ).getBytes();

    @Test
    public void run() throws Exception
    {

        String changelog = "Create test repo";

        IndyFoloContentClientModule folo = client.module( IndyFoloContentClientModule.class );
        IndyPromoteClientModule promote = client.module( IndyPromoteClientModule.class );
        IndyFoloAdminClientModule foloAdmin = client.module( IndyFoloAdminClientModule.class );

        // 0. Create tracking id and use it as promotion source
        String trackingId = newName();

        // 1. Create source and target repositories
        HostedRepository source = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, trackingId );
        client.stores().create( source, changelog, HostedRepository.class );

        HostedRepository target = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "target" );
        client.stores().create( target, changelog, HostedRepository.class );

        // 2. Store the artifact to source repo
        InputStream stream = new ByteArrayInputStream( bytes );
        folo.store( trackingId, source.getKey(), path, stream );

        // 3. Seal the record
        foloAdmin.sealTrackingRecord( trackingId );

        // 4. Promote to target repo (by path)
        PathsPromoteRequest promoteRequest = new PathsPromoteRequest( source.getKey(), target.getKey() );
        promoteRequest.setFireEvents( true );
        promote.promoteByPath( promoteRequest );

        // 5. Check tracking record, the store is adjusted to target
        TrackedContentDTO trackingContent = foloAdmin.getRawTrackingContent( trackingId );
        List<TrackedContentEntryDTO> list = new ArrayList<>( trackingContent.getUploads() );
        TrackedContentEntryDTO trackedContent = list.get( 0 );
        assertEquals( target.getKey(), trackedContent.getStoreKey() );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.<IndyClientModule>asList( new IndyFoloContentClientModule(), new IndyFoloAdminClientModule(),
                                                new IndyPromoteClientModule() );
    }

    @Override
    protected void initTestData( CoreServerFixture fixture ) throws IOException
    {
        writeDataFile( "promote/change/tracking-id-formatter.groovy",
                       readTestResource( getClass().getSimpleName() + "/tracking-id-formatter.groovy" ) );
    }
}
