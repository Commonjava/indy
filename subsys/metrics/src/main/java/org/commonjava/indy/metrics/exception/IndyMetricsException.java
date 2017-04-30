package org.commonjava.indy.metrics.exception;

import org.commonjava.indy.IndyException;

/**
 * Created by xiabai on 5/9/17.
 */
public class IndyMetricsException extends IndyException
{
    public IndyMetricsException( final String message, final Object... params )
    {
        super( message, params );
    }

    public IndyMetricsException( final String message, final Throwable cause, final Object... params )
    {
        super( message, cause, params );
    }

    private static final long serialVersionUID = 1L;
}
