/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.couch.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.couch.model.DenormalizationException;
import org.commonjava.web.common.ser.JsonSerializer;
import org.junit.Test;

public class RepositoryDocTest
{

    private final JsonSerializer ser = new JsonSerializer( new StoreKeySerializer() );

    @Test
    public void denormalizeCouchDocumentIdFromName()
        throws DenormalizationException
    {
        final RepositoryDoc repo = new RepositoryDoc( "test", "http://www.nowhere.com/" );
        repo.calculateDenormalizedFields();

        assertThat( repo.getCouchDocId(), equalTo( namespaceId( StoreType.repository.name(), "test" ) ) );
    }

    @Test
    public void denormalizeAuthInfoFromUrl()
        throws DenormalizationException
    {
        final RepositoryDoc repo = new RepositoryDoc( "test", "http://admin:admin123@www.nowhere.com/" );
        repo.calculateDenormalizedFields();

        assertThat( repo.getUrl(), equalTo( "http://www.nowhere.com/" ) );
        assertThat( repo.getUser(), equalTo( "admin" ) );
        assertThat( repo.getPassword(), equalTo( "admin123" ) );
    }

    @Test
    public void roundTrip()
    {
        final RepositoryDoc doc = new RepositoryDoc( "test", "http://admin:admin@www.nowhere.com:8080/" );
        doc.calculateDenormalizedFields();
        doc.setCouchDocRev( "2345" );
        doc.setTimeoutSeconds( 10 );
        final String json = ser.toString( doc );

        System.out.println( "JSON:\n\n" + json + "\n\n" );

        @SuppressWarnings( "unchecked" )
        final RepositoryDoc result = ser.fromString( json, RepositoryDoc.class );

        assertThat( result.getCouchDocId(), equalTo( doc.getCouchDocId() ) );
        assertThat( result.getCouchDocRev(), equalTo( doc.getCouchDocRev() ) );
        assertThat( result.getHost(), equalTo( doc.getHost() ) );
        assertThat( result.getKey(), equalTo( doc.getKey() ) );
        assertThat( result.getName(), equalTo( doc.getName() ) );
        assertThat( result.getPassword(), equalTo( doc.getPassword() ) );
        assertThat( result.getPort(), equalTo( doc.getPort() ) );
        assertThat( result.getTimeoutSeconds(), equalTo( doc.getTimeoutSeconds() ) );
        assertThat( result.getUrl(), equalTo( doc.getUrl() ) );
        assertThat( result.getUser(), equalTo( doc.getUser() ) );
    }

}
