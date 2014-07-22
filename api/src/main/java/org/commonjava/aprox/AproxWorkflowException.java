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
package org.commonjava.aprox;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.text.MessageFormat;

import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.util.ApplicationStatus;

/**
 * Signals an error between the REST-resources layer and the next layer down (except for {@link FileManager}, which is normally two layers down thanks 
 * to binding controllers). Workflow exceptions are intended to carry with them some notion of what response to send to the user (even if it's 
 * the default: HTTP 500).
 */
public class AproxWorkflowException
    extends Exception
{
    private Object[] params;

    private transient String formattedMessage;

    private int status;

    public AproxWorkflowException( final String message, final Object... params )
    {
        super( message );
        this.params = params;
    }

    public AproxWorkflowException( final String message, final Throwable cause, final Object... params )
    {
        super( message, cause );
        this.params = params;
    }

    public AproxWorkflowException( final ApplicationStatus status, final String message, final Object... params )
    {
        super( message );
        this.params = params;
        this.status = status.code();
    }

    public AproxWorkflowException( final int status, final String message, final Object... params )
    {
        super( message );
        this.params = params;
        this.status = status;
    }

    private static final long serialVersionUID = 1L;

    @Override
    public synchronized String getMessage()
    {
        if ( formattedMessage == null )
        {
            final String format = super.getMessage();
            if ( params == null || params.length < 1 )
            {
                formattedMessage = format;
            }
            else
            {
                final String original = formattedMessage;
                try
                {
                    formattedMessage = String.format( format.replaceAll( "\\{\\}", "%s" ), params );
                }
                catch ( final Error e )
                {
                }
                catch ( final RuntimeException e )
                {
                }
                catch ( final Exception e )
                {
                }

                if ( formattedMessage == null || original == formattedMessage )
                {
                    try
                    {
                        formattedMessage = MessageFormat.format( format, params );
                    }
                    catch ( final Error e )
                    {
                        formattedMessage = format;
                        throw e;
                    }
                    catch ( final RuntimeException e )
                    {
                        formattedMessage = format;
                        throw e;
                    }
                    catch ( final Exception e )
                    {
                        formattedMessage = format;
                    }
                }
            }
        }

        return formattedMessage;
    }

    public int getStatus()
    {
        return status < 1 ? ApplicationStatus.BAD_REQUEST.code() : status;
    }

    /**
     * Stringify all parameters pre-emptively on serialization, to prevent {@link NotSerializableException}.
     * Since all parameters are used in {@link String#format} or {@link MessageFormat#format}, flattening them
     * to strings is an acceptable way to provide this functionality without making the use of {@link Serializable}
     * viral.
     */
    private Object writeReplace()
    {
        final Object[] newParams = new Object[params.length];
        int i = 0;
        for ( final Object object : params )
        {
            newParams[i] = String.valueOf( object );
            i++;
        }

        this.params = newParams;
        return this;
    }

}
