package org.commonjava.indy.promote.ftest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.commonjava.test.http.expect.ExpectationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by ruhan on 12/11/18.
 */
public class AsyncExpectationHandler implements ExpectationHandler
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void handle( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse )
                    throws ServletException, IOException
    {
        String jsonBody = IOUtils.toString( httpServletRequest.getInputStream() );
        /*ObjectMapper mapper = new ObjectMapper();
        GroupPromoteResult promoteResult = mapper.readValue( jsonBody, GroupPromoteResult.class );*/
        logger.debug( ">>>>>>>>>>>>>>>>\n\n" + jsonBody + "\n\n" );
        httpServletResponse.setStatus( 200 );
        callbackReceived = true;
        notifyTester();
    }

    volatile boolean callbackReceived;

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
