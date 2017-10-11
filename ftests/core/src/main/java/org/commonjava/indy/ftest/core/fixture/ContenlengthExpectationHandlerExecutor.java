package org.commonjava.indy.ftest.core.fixture;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class ContenlengthExpectationHandlerExecutor
                implements ExpectationHandlerExecutor
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * How many times for call of the the handler
     */
    private int countIndex;

    /**
     * When countIndex equal to breakIndex, the handler will broken
     */
    private int breakIndex;

    private String responseContent;

    public ContenlengthExpectationHandlerExecutor( int countIndex, int breakIndex, String responseContent )
    {
        this.countIndex = countIndex;
        this.breakIndex = breakIndex;
        this.responseContent = responseContent;
    }

    public int getCountIndex()
    {
        return countIndex;
    }

    public void setCountIndex( int countIndex )
    {
        this.countIndex = countIndex;
    }

    public int getBreakIndex()
    {
        return breakIndex;
    }

    public void setBreakIndex( int breakIndex )
    {
        this.breakIndex = breakIndex;
    }

    public String getResponseContent()
    {
        return responseContent;
    }

    public void setResponseContent( String responseContent )
    {
        this.responseContent = responseContent;
    }

    @Override
    public void execute( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        OutputStream out = response.getOutputStream();
        if ( countIndex < breakIndex )
        {

            try
            {
                logger.info( "ContenlengthExpectationHandlerExecutor call index =" + countIndex + " url:"
                                             + request.getRequestURI() );
                ExceptionInputStream.copy( new ExceptionInputStream( responseContent.getBytes() ), out );
                ++countIndex;
            }
            catch ( Throwable t )
            {
                throw new ServletException( t.getMessage() );
            }

        }
        else
        {
            logger.info( "ContenlengthExpectationHandlerExecutor call index =  " + countIndex + " url:"
                                         + request.getRequestURI() );
            IOUtils.write( responseContent, out );
        }
    }
}
