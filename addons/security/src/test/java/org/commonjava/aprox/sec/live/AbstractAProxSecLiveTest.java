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
package org.commonjava.aprox.sec.live;

import static org.apache.commons.io.IOUtils.copy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringWriter;

import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.user.web.test.AbstractUserRESTCouchTest;
import org.junit.After;
import org.junit.Before;

public class AbstractAProxSecLiveTest
    extends AbstractUserRESTCouchTest
{

    @Inject
    protected ProxyDataManager proxyManager;

    @Inject
    @AproxData
    protected CouchChangeListener changeListener;

    @Inject
    @AproxData
    private CouchManager couch;

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
        changeListener.startup();
    }

    @After
    public final void teardownAProxLiveTest()
        throws Exception
    {
        changeListener.shutdown();
        while ( changeListener.isRunning() )
        {
            synchronized ( changeListener )
            {
                System.out.println( "Waiting 2s for change listener to shutdown..." );
                changeListener.wait( 2000 );
            }
        }

        couch.dropDatabase();
    }

    protected String getString( final String url, final int expectedStatus )
        throws ClientProtocolException, IOException
    {
        final HttpResponse response = http.execute( new HttpGet( url ) );
        final StatusLine sl = response.getStatusLine();

        assertThat( sl.getStatusCode(), equalTo( expectedStatus ) );
        assertThat( response.getEntity(), notNullValue() );

        final StringWriter sw = new StringWriter();
        copy( response.getEntity()
                      .getContent(), sw );

        return sw.toString();
    }

    @Override
    protected CouchManager getCouchManager()
    {
        return couch;
    }

}
