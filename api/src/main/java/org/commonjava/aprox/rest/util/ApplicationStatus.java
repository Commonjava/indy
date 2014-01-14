package org.commonjava.aprox.rest.util;

public enum ApplicationStatus
{

    /* @formatter:off */
    OK( 200, "Ok" ), 
    CREATED( 201, "Created" ), 
    
    MOVED_PERMANENTLY( 301, "Moved Permanently" ),
    FOUND( 302, "Found" ),
    
    NOT_MODIFIED( 304, "Not Modified" ),
    
    BAD_REQUEST( 400, "Bad Request" ), 
    
    NOT_FOUND( 404, "Not Found" ), 
    
    CONFLICT( 409, "Conflict" ),
    
    SERVER_ERROR( 500, "Internal Server Error" );
    /* @formatter:on */

    private int status;

    private String message;

    private ApplicationStatus( final int status, final String messsage )
    {
        this.status = status;
        this.message = messsage;
    }

    public int code()
    {
        return status;
    }

    public String message()
    {
        return message;
    }

}
