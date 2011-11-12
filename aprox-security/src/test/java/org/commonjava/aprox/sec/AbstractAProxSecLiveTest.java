/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.sec;

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
