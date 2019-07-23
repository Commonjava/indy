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
package org.commonjava.indy.pkg.maven.content;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.pkg.maven.content.MavenContentsFilteringTransferDecorator;
import org.commonjava.indy.test.fixture.core.HttpTestFixture;
import org.commonjava.maven.galley.config.TransportMetricConfig;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.transport.htcli.internal.HttpDownload;
import org.commonjava.maven.galley.transport.htcli.model.SimpleHttpLocation;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MavenContentFilteringTransferDecoratorTest
{
    @Rule
    public HttpTestFixture fixture = new HttpTestFixture( "test", new MavenContentsFilteringTransferDecorator() );

    private static MetricRegistry metricRegistry = new MetricRegistry();

    private static TransportMetricConfig metricConfig = new TransportMetricConfig()
    {
        @Override
        public boolean isEnabled()
        {
            return true;
        }

        @Override
        public String getNodePrefix()
        {
            return null;
        }

        @Override
        public String getMetricUniqueName( Location location )
        {
            if ( location.getName().equals( "test" ) )
            {
                return location.getName();
            }
            return null;
        }
    };

    @Test
    public void metadataFilteringWhenSnapshotsNotAllowed() throws Exception
    {
        final String fname = "/commons-codec/commons-codec/maven-metadata.xml";

        // @formatter:off
        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<metadata modelVersion=\"1.1.0\">"
            + "  <groupId>commons-codec</groupId>"
            + "  <artifactId>commons-codec</artifactId>"
            + "  <versioning>"
            + "    <latest>1.2</latest>"
            + "    <release>1.2</release>"
            + "    <versions>"
            + "      <version>1.1</version>"
            + "      <version>1.1-SNAPSHOT</version>"
            + "      <version>1.2</version>"
            + "    </versions>"
            + "    <lastUpdated>20171020231327</lastUpdated>"
            + "  </versioning>"
            + "</metadata>";
        // @formatter:on

        final String baseUri = fixture.getBaseUri();
        final SimpleHttpLocation location = new SimpleHttpLocation( "test", baseUri, false, true, true, true, null );
        final Transfer transfer = fixture.getTransfer( new ConcreteResource( location, fname ) );
        final String url = fixture.formatUrl( fname );

        assertThat( transfer.exists(), equalTo( false ) );

        try (OutputStream stream = transfer.openOutputStream( TransferOperation.UPLOAD ))
        {
            IOUtils.write( content, stream );
        }

        try (InputStream in = transfer.openInputStream())
        {
            List<String> filtered = IOUtils.readLines( in );
            assertThat( filtered, notNullValue() );
            StringBuilder builder = new StringBuilder();
            filtered.forEach( builder::append );
            String result = builder.toString();
            assertThat( result.contains( "1.1" ), equalTo( true ) );
            assertThat( result.contains( "1.2" ), equalTo( true ) );
            assertThat( result.contains( "1.1-SNAPTHOT" ), equalTo( false ) );
        }
    }

    @Test
    public void snapshotNotExistsWhenSnapshotsNotAllowed()
            throws Exception
    {
        final String fname = "/commons-codec/commons-codec/11-SNAPSHOT/maven-metadata.xml.md5";
        final String content = "kljsjdlfkjsdlkj123j13=20=s0dfjklxjkj";
        Transfer result = getTestHttpTransfer( fname, content );
        assertThat( result.exists(), equalTo( false ) );
    }


    @Test
    public void artifactNotExistsWhenSnapshotsNotAllowed()
            throws Exception
    {
        final String fname = "/commons-codec/commons-codec/11-SNAPSHOT/commons-codec-11-SNAPSHOT.jar";
        final String content = "This is a jar";
        Transfer result = getTestHttpTransfer( fname, content );
        assertThat( result.exists(), equalTo( false ) );
    }

    private Transfer getTestHttpTransfer(final String path, final String content) throws Exception{
        fixture.expect( "GET", fixture.formatUrl( path ), 200, content );

        final String baseUri = fixture.getBaseUri();
        final SimpleHttpLocation location = new SimpleHttpLocation( "test", baseUri, false, true, true, true, null );
        final Transfer transfer = fixture.getTransfer( new ConcreteResource( location, path ) );
        final String url = fixture.formatUrl( path );

        assertThat( transfer.exists(), equalTo( false ) );

        HttpDownload dl = new HttpDownload( url, location, transfer, new HashMap<Transfer, Long>(), new EventMetadata(),
                                            fixture.getHttp().getHttp(), new ObjectMapper(), metricRegistry, metricConfig );

        return dl.call().getTransfer();
    }

    @Test
    public void releaseListingInWhenSnapshotsNotAllowedWithVersionPath()
            throws Exception
    {
        final String fname = "commons-codec/commons-codec/1.1/";
        final SimpleHttpLocation location =
                new SimpleHttpLocation( "test", "http://test", false, true, false, false, null );
        final ConcreteResource resource = new ConcreteResource( location, fname );
        final Transfer transfer = new Transfer( resource, null, null, null );
        final List<String> listElems =
                Arrays.asList( "commons-codec-1.1.jar", "commons-codec-1.1-source.jar", "maven-metadata.xml" );

        String[] listing = listElems.toArray( new String[3] );
        MavenContentsFilteringTransferDecorator decorator = new MavenContentsFilteringTransferDecorator();
        listing = decorator.decorateListing( transfer, listing, new EventMetadata() );

        assertThat( listing, CoreMatchers.<String[]>notNullValue() );
        assertThat( listing.length, equalTo( 3 ) );
        assertThat( Arrays.asList( listing ).containsAll( listElems ), equalTo( true ) );
    }

    @Test
    public void snapshotListingNotInWhenSnapshotsNotAllowedWithNoVersionPath()
            throws Exception
    {
        final String fname = "commons-codec/commons-codec/";
        final SimpleHttpLocation location =
                new SimpleHttpLocation( "test", "http://test", false, true, false, false, null );
        final ConcreteResource resource = new ConcreteResource( location, fname );
        final Transfer transfer = new Transfer( resource, null, null, null );

        String[] listing = Arrays.asList( "1.0/", "1.0-SNAPSHOT/", "1.1/", "1.1-SNAPSHOT/" ).toArray( new String[4] );
        MavenContentsFilteringTransferDecorator decorator = new MavenContentsFilteringTransferDecorator();
        listing = decorator.decorateListing( transfer, listing, new EventMetadata() );

        System.out.println( Arrays.asList( listing ) );

        assertThat( listing, CoreMatchers.<String[]>notNullValue() );
        assertThat( listing.length, equalTo( 2 ) );
        assertThat( Arrays.asList( listing ).contains( "1.0-SNAPSHOT/" ), equalTo( false ) );
        assertThat( Arrays.asList( listing ).contains( "1.1-SNAPSHOT/" ), equalTo( false ) );
    }

    @Test
    public void snapshotListingNotInWhenSnapshotsNotAllowedWithVersionPath()
            throws Exception
    {
        final String fname = "commons-codec/commons-codec/1.1-SNAPSHOT/";
        final SimpleHttpLocation location =
                new SimpleHttpLocation( "test", "http://test", false, true, false, false, null );
        final ConcreteResource resource = new ConcreteResource( location, fname );
        final Transfer transfer = new Transfer( resource, null, null, null );

        String[] listing = Arrays.asList( "commons-codec-1.1-SNAPSHOT.jar", "commons-codec-1.1-SNAPSHOT-source.jar",
                                          "maven-metadata.xml" ).toArray( new String[4] );
        MavenContentsFilteringTransferDecorator decorator = new MavenContentsFilteringTransferDecorator();
        listing = decorator.decorateListing( transfer, listing, new EventMetadata() );

        assertThat( listing, CoreMatchers.<String[]>notNullValue() );
        assertThat( listing.length, equalTo( 0 ) );
    }

}
