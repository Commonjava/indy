package org.commonjava.aprox.core.filer;

import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_NFC_EVENT;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.filer.NotFoundCache;
import org.commonjava.shelflife.expire.ExpirationManager;
import org.commonjava.shelflife.expire.ExpirationManagerException;
import org.commonjava.shelflife.expire.match.ExpirationMatcher;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;

@ApplicationScoped
public class DefaultNotFoundCache
    implements NotFoundCache
{

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private AproxConfiguration config;

    protected DefaultNotFoundCache()
    {
    }

    public DefaultNotFoundCache( final ExpirationManager expirationManager, final AproxConfiguration config )
    {
        this.expirationManager = expirationManager;
        this.config = config;
    }

    @Override
    public void addMissing( final String url )
        throws ProxyDataException
    {
        final long millis = TimeUnit.MILLISECONDS.convert( config.getNotFoundCacheTimeoutSeconds(), TimeUnit.SECONDS );
        try
        {
            expirationManager.schedule( createNFCExpiration( url, millis ) );
        }
        catch ( final ExpirationManagerException e )
        {
            throw new ProxyDataException( "Failed to schedule NFC expiration for: %s. Reason: %s", e, url,
                                          e.getMessage() );
        }
    }

    private Expiration createNFCExpiration( final String url, final long timeout )
    {
        return new Expiration( createNFCExpirationKey( url ), System.currentTimeMillis() + timeout, url );
    }

    private ExpirationKey createNFCExpirationKey( final String url )
    {
        final String pathHash = DigestUtils.md5Hex( url );

        return new ExpirationKey( APROX_EVENT, APROX_NFC_EVENT, url, pathHash );
    }

    @Override
    public void clearExactMissing( final String url )
        throws ProxyDataException
    {
        try
        {
            expirationManager.cancel( createNFCExpirationKey( url ) );
        }
        catch ( final ExpirationManagerException e )
        {
            throw new ProxyDataException( "Failed to clear NFC entry for: %s. Reason: %s", e, url, e.getMessage() );
        }
    }

    @Override
    public void clearMissingForBaseUrl( final String baseUrl )
        throws ProxyDataException
    {
        try
        {
            expirationManager.cancelAll( new ExpirationMatcher()
            {
                @Override
                public boolean matches( final Expiration expiration )
                {
                    final String url = (String) expiration.getData();
                    return url.startsWith( baseUrl );
                }

                @Override
                public String formatQuery()
                {
                    return "NFC Expirations with baseUrl: " + baseUrl;
                }
            } );
        }
        catch ( final ExpirationManagerException e )
        {
            throw new ProxyDataException( "Failed to clear all NFC entries with baseUrl: %s. Reason: %s", e, baseUrl,
                                          e.getMessage() );
        }
    }

    @Override
    public void clearAllMissing()
        throws ProxyDataException
    {
        try
        {
            expirationManager.cancelAll();
        }
        catch ( final ExpirationManagerException e )
        {
            throw new ProxyDataException( "Failed to clear all NFC entries. Reason: %s", e, e.getMessage() );
        }
    }

    @Override
    public boolean hasEntry( final String url )
    {
        return expirationManager.hasExpiration( createNFCExpirationKey( url ) );
    }

}
