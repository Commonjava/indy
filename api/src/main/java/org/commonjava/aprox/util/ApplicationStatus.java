/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.util;

public enum ApplicationStatus
{

    /* @formatter:off */
    OK( 200, "Ok" ), 
    CREATED( 201, "Created" ), 
    NO_CONTENT(204, "No Content"),
    
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

    public static ApplicationStatus getStatus( final int status )
    {
        for ( final ApplicationStatus as : values() )
        {
            if ( as.code() == status )
            {
                return as;
            }
        }

        return null;
    }

}
