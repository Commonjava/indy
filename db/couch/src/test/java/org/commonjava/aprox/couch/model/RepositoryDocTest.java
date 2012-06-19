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

import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.couch.model.DenormalizationException;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Test;

public class RepositoryDocTest
{

    private final JsonSerializer ser = new JsonSerializer( new StoreKeySerializer() );

    @Test
    public void denormalizeCouchDocumentIdFromName()
        throws DenormalizationException
    {
        final RepositoryDoc repo = new RepositoryDoc( new Repository( "test", "http://www.nowhere.com/" ) );
        repo.calculateDenormalizedFields();

        assertThat( repo.getCouchDocId(), equalTo( namespaceId( StoreType.repository.name(), "test" ) ) );
    }

    @Test
    public void denormalizeAuthInfoFromUrl()
        throws DenormalizationException
    {
        final RepositoryDoc repo =
            new RepositoryDoc( new Repository( "test", "http://admin:admin123@www.nowhere.com/" ) );
        repo.calculateDenormalizedFields();

        assertThat( repo.exportStore()
                        .getUrl(), equalTo( "http://www.nowhere.com/" ) );
        assertThat( repo.exportStore()
                        .getUser(), equalTo( "admin" ) );
        assertThat( repo.exportStore()
                        .getPassword(), equalTo( "admin123" ) );
    }

    @Test
    public void roundTrip()
    {
        final RepositoryDoc doc =
            new RepositoryDoc( new Repository( "test", "http://admin:admin@www.nowhere.com:8080/" ) );
        doc.calculateDenormalizedFields();
        doc.setCouchDocRev( "2345" );
        final Repository repo = doc.exportStore();
        repo.setTimeoutSeconds( 10 );

        final String json = ser.toString( doc );

        System.out.println( "JSON:\n\n" + json + "\n\n" );

        @SuppressWarnings( "unchecked" )
        final RepositoryDoc resultDoc = ser.fromString( json, RepositoryDoc.class );
        final Repository result = resultDoc.exportStore();

        assertThat( resultDoc.getCouchDocId(), equalTo( doc.getCouchDocId() ) );
        assertThat( resultDoc.getCouchDocRev(), equalTo( doc.getCouchDocRev() ) );
        assertThat( result.getHost(), equalTo( repo.getHost() ) );
        assertThat( result.getKey(), equalTo( repo.getKey() ) );
        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getPassword(), equalTo( repo.getPassword() ) );
        assertThat( result.getPort(), equalTo( repo.getPort() ) );
        assertThat( result.getTimeoutSeconds(), equalTo( repo.getTimeoutSeconds() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), equalTo( repo.getUser() ) );
    }

}
