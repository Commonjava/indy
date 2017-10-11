package org.commonjava.indy.ftest.core.fixture;

import org.apache.commons.lang3.tuple.Pair;
import org.commonjava.test.http.expect.ExpectationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockExpectationHandler
                implements ExpectationHandler
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Adds a response header with the given name and
     * integer value.
     */
    private List<Pair<String, Integer>> intHeaders;

    private String characterEncoding;

    private String contentType;

    /**
     *
     * Sets a response header with the given name and value.
     *
     */
    private List<Pair<String, String>> stringHeaders;

    /**
     *
     * Sets a response header with the given name and
     * date-value.
     *
     */
    private List<Pair<String, Long>> dateHeaders;

    private int status;

    private ExpectationHandlerExecutor expectationHandlerExecutor;

    private MockExpectationHandler( List<Pair<String, Integer>> intHeaders, String characterEncoding,
                                    String contentType, List<Pair<String, String>> stringHeaders,
                                    List<Pair<String, Long>> dateHeaders, int status,
                                    ExpectationHandlerExecutor expectationHandlerExecutor )
    {
        this.characterEncoding = characterEncoding;
        this.contentType = contentType;
        this.intHeaders = intHeaders;
        this.stringHeaders = stringHeaders;
        this.dateHeaders = dateHeaders;
        this.status = status;
        this.expectationHandlerExecutor = expectationHandlerExecutor;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus( int status )
    {
        this.status = status;
    }

    public static Builder getBuilder()
    {
        return new Builder();
    }

    public static class Builder
    {

        private List<Pair<String, Integer>> intHeaders;

        private String characterEncoding;

        private String contentType;

        private List<Pair<String, String>> stringHeaders;

        private List<Pair<String, Long>> dateHeaders;

        private int status;

        private ExpectationHandlerExecutor expectationHandlerExecutor;

        public Builder intHeaders( Pair<String, Integer> intHeader )
        {
            if ( this.intHeaders == null )
            {
                this.intHeaders = new ArrayList<Pair<String, Integer>>();
            }

            this.intHeaders.add( intHeader );
            return this;
        }

        public Builder characterEncoding( String characterEncoding )
        {
            this.characterEncoding = characterEncoding;
            return this;
        }

        public Builder contentType( String contentType )
        {
            this.contentType = contentType;
            return this;
        }

        public Builder stringHeaders( Pair<String, String> stringHeader )
        {
            if ( this.stringHeaders == null )
            {
                this.stringHeaders = new ArrayList<Pair<String, String>>();
            }

            this.stringHeaders.add( stringHeader );
            return this;
        }

        public Builder dateHeaders( Pair<String, Long> dateHeader )
        {
            if ( this.dateHeaders == null )
            {
                this.dateHeaders = new ArrayList<Pair<String, Long>>();
            }

            this.dateHeaders.add( dateHeader );
            return this;
        }

        public Builder status( int status )
        {
            this.status = status;
            return this;
        }

        public Builder expectationHandlerExecutor( ExpectationHandlerExecutor expectationHandlerExecutor )
        {
            this.expectationHandlerExecutor = expectationHandlerExecutor;
            return this;
        }

        public MockExpectationHandler build()
        {
            return new MockExpectationHandler( this.intHeaders, this.characterEncoding, this.contentType,
                                               this.stringHeaders, this.dateHeaders, this.status,
                                               this.expectationHandlerExecutor );
        }
    }

    @Override
    public void handle( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        logger.debug( "MockExpectationHandler.handle call url:{}",request.getContextPath() );
        if ( this.characterEncoding != null )
        {
            response.setCharacterEncoding( this.characterEncoding );
        }
        if ( this.contentType != null )
        {
            response.setContentType( this.contentType );
        }
        if ( this.intHeaders != null )
        {
            intHeaders.forEach( pair -> {
                response.setIntHeader( pair.getKey(), pair.getValue() );
            } );
        }
        if ( this.stringHeaders != null )
        {
            stringHeaders.forEach( pair -> {
                response.setHeader( pair.getKey(), pair.getValue() );
            } );
        }
        if ( this.dateHeaders != null )
        {
            dateHeaders.forEach( pair -> {
                response.setDateHeader( pair.getKey(), pair.getValue() );
            } );
        }
        if ( this.status != 0 )
        {
            response.setStatus( this.getStatus() );
        }

        expectationHandlerExecutor.execute( request, response );
    }

}
