package org.commonjava.aprox.core.change.sl;

import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_NFC_EVENT;

import org.commonjava.shelflife.expire.match.PrefixMatcher;

public class NFCMatcher
    extends PrefixMatcher
{

    public NFCMatcher( final String url )
    {
        super( APROX_EVENT, APROX_NFC_EVENT, url );
    }

    public NFCMatcher()
    {
        super( APROX_EVENT, APROX_NFC_EVENT );
    }

}
