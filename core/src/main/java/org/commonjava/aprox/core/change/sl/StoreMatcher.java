package org.commonjava.aprox.core.change.sl;

import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_FILE_EVENT;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.shelflife.expire.match.PrefixMatcher;

public class StoreMatcher
    extends PrefixMatcher
{

    public StoreMatcher( final StoreKey key )
    {
        super( APROX_EVENT, APROX_FILE_EVENT, key.getType()
                                                 .name(), key.getName() );
    }

    public StoreMatcher()
    {
        super( APROX_EVENT, APROX_FILE_EVENT );
    }

}
