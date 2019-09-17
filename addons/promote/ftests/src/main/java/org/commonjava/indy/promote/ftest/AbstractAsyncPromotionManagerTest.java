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
package org.commonjava.indy.promote.ftest;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.promote.model.AbstractPromoteRequest;
import org.commonjava.indy.promote.model.AbstractPromoteResult;
import org.commonjava.indy.promote.model.CallbackTarget;
import org.commonjava.test.http.expect.ExpectationHandler;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruhan on 12/18/18.
 */
public class AbstractAsyncPromotionManagerTest<T extends AbstractPromoteRequest, R extends AbstractPromoteResult>
                extends AbstractPromotionManagerTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "test" );

    protected String callbackUrl;

    protected AsyncExpectationHandler handler;

    @Before
    public void setupCallbackHandle() throws Exception
    {
        callbackUrl = server.formatUrl( "foo/bar/callback" );
        if ( isRetry() )
        {
            handler = new AsyncExpectationRetryHandler();
        }
        else
        {
            handler = new AsyncExpectationHandler();
        }
        server.expect( "POST", callbackUrl, handler );
    }

    protected T getAsyncRequest( AbstractPromoteRequest request )
    {
        request.setAsync( true );
        Map<String, String> headers = new HashMap<>(  );
        headers.put( "Authorization", "Bearer I490M5M0057HSU..." );
        request.setCallback( new CallbackTarget( callbackUrl, headers ) );
        return (T) request;
    }

    public R getAsyncPromoteResult( Class<R> resultClass ) throws IOException
    {
        handler.waitComplete();
        return handler.getPromoteResult( resultClass );
    }


    private class AsyncExpectationHandler implements ExpectationHandler
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        final IndyObjectMapper mapper = new IndyObjectMapper( true );

        private String jsonBody;

        @Override
        public void handle( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse )
                        throws ServletException, IOException
        {
            jsonBody = IOUtils.toString( httpServletRequest.getInputStream() );
            logger.debug( ">>>>>>>>>>>>>>>>\n\n" + jsonBody + "\n\n" );
            httpServletResponse.setStatus( 200 );
            callbackReceived = true;
            notifyTester();
        }

        volatile boolean callbackReceived;

        public String getJsonBody()
        {
            return jsonBody;
        }

        public <T> T getPromoteResult( Class<T> resultClass ) throws IOException
        {
            if ( jsonBody == null )
            {
                return null;
            }
            return mapper.readValue( jsonBody, resultClass );
        }

        public synchronized void waitComplete()
        {
            if ( callbackReceived )
            {
                return;
            }

            try
            {
                if ( isRetry() )
                {
                    wait( 120 * 1000 ); // the minimum retry backoff is 1 min so we wait maximum 120 seconds
                }
                else
                {
                    wait( 30 * 1000 ); // max 30 seconds
                }
            }
            catch ( InterruptedException e )
            {
                ;
            }
        }

        private synchronized void notifyTester()
        {
            notifyAll();
        }
    }

    private class AsyncExpectationRetryHandler extends AsyncExpectationHandler
    {
        boolean isFirstTime = true; // we return 500 error for the first time to force promote to retry

        @Override
        public void handle( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse )
                        throws ServletException, IOException
        {
            if ( isFirstTime )
            {
                httpServletResponse.setStatus( 500 );
                isFirstTime = false;
            }
            else
            {
                super.handle( httpServletRequest, httpServletResponse );
            }
        }

    }

    // if true, we will use AsyncExpectationRetryHandler to test retry
    protected boolean isRetry()
    {
        return false;
    }

}
