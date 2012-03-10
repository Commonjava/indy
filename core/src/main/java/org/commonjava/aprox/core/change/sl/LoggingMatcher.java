package org.commonjava.aprox.core.change.sl;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.shelflife.expire.match.ExpirationMatcher;
import org.commonjava.shelflife.model.Expiration;

public class LoggingMatcher
    implements ExpirationMatcher
{

    private final ExpirationMatcher delegate;

    private final List<Expiration> matching = new ArrayList<Expiration>();

    private final List<Expiration> nonMatching = new ArrayList<Expiration>();

    public LoggingMatcher( final ExpirationMatcher delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public boolean matches( final Expiration expiration )
    {
        final boolean matches = delegate.matches( expiration );
        if ( matches )
        {
            matching.add( expiration );
        }
        else
        {
            nonMatching.add( expiration );
        }

        return matches;
    }

    public List<Expiration> getMatching()
    {
        return matching;
    }

    public List<Expiration> getNonMatching()
    {
        return nonMatching;
    }

    @Override
    public String formatQuery()
    {
        return "Logged [" + delegate + "]";
    }

}
