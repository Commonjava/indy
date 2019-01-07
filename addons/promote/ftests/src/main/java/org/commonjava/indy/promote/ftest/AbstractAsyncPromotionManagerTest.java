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
        handler = new AsyncExpectationHandler();
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
                wait( 30 * 1000 ); // max 30 seconds
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

}
