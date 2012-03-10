package org.commonjava.aprox.core.change.sl;

import java.util.Date;

import org.commonjava.shelflife.expire.match.ExpirationMatcher;
import org.commonjava.shelflife.model.Expiration;

public class MaxTimeoutMatcher
    implements ExpirationMatcher
{

    private final long maxTimeout;

    public MaxTimeoutMatcher( final long maxTimeout )
    {
        this.maxTimeout = maxTimeout;
    }

    @Override
    public boolean matches( final Expiration expiration )
    {
        final long expires = expiration.getExpires();
        return expires <= maxTimeout;
    }

    @Override
    public String formatQuery()
    {
        return "expires <= " + new Date( maxTimeout );
    }

}
