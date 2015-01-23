package org.commonjava.aprox.promote.data;

import org.commonjava.aprox.AproxException;

public class PromotionException
    extends AproxException
{

    private static final long serialVersionUID = 1L;

    public PromotionException( final String message, final Object... params )
    {
        super( message, params );
    }

    public PromotionException( final String message, final Throwable cause, final Object... params )
    {
        super( message, cause, params );
    }

}
