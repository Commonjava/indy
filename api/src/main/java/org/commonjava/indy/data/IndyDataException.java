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
package org.commonjava.indy.data;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.text.MessageFormat;

import org.commonjava.indy.model.core.ArtifactStore;

/**
 * Exception that indicates an error occurred while retrieving or managing configuration data about one or more
 * {@link ArtifactStore} instances.
 */
public class IndyDataException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    private Object[] params;

    private int status;

    public IndyDataException( final String message, final Throwable cause, final Object... params )
    {
        super( message, cause );
        this.params = params;
    }

    public IndyDataException( final String message, final Object... params )
    {
        super( message );
        this.params = params;
    }

    public IndyDataException( final int status, final String message, final Throwable cause, final Object... params )
    {
        super( message, cause );
        this.params = params;
        this.status = status;
    }

    public IndyDataException( final int status, final String message, final Object... params )
    {
        super( message );
        this.params = params;
        this.status = status;
    }

    public int getStatus()
    {
        return status;
    }

    @Override
    public String getMessage()
    {
        String format = super.getMessage();

        if ( format == null || params == null || params.length < 1 )
        {
            return format;
        }

        String formattedMessage = null;
        format = format.replaceAll( "\\{\\}", "%s" );

        try
        {
            formattedMessage = String.format( format, params );
        }
        catch ( final Throwable e )
        {
            try
            {
                formattedMessage = MessageFormat.format( format, params );
            }
            catch ( Throwable ex )
            {
                formattedMessage = format;
            }
        }

        return formattedMessage;
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
