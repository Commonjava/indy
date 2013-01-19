package org.commonjava.aprox.filer;

import org.commonjava.aprox.data.ProxyDataException;

public interface NotFoundCache
{

    void addMissing( String url )
        throws ProxyDataException;

    void clearExactMissing( String url )
        throws ProxyDataException;

    void clearMissingForBaseUrl( String baseUrl )
        throws ProxyDataException;

    void clearAllMissing()
        throws ProxyDataException;

    boolean hasEntry( String url );

}
