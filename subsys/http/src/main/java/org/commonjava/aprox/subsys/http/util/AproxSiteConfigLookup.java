package org.commonjava.aprox.subsys.http.util;

import static org.commonjava.util.jhttpc.auth.AttributePasswordManager.*;
import static org.commonjava.util.jhttpc.auth.PasswordType.*;

import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.model.galley.GroupLocation;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.auth.PasswordEntry;
import org.commonjava.util.jhttpc.auth.AttributePasswordManager;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Created by jdcasey on 11/9/15.
 */
public class AproxSiteConfigLookup
        implements AttributePasswordManager.SiteConfigLookup
{

    @Inject
    private StoreDataManager storeDataManager;

    protected AproxSiteConfigLookup()
    {
    }

    public AproxSiteConfigLookup( StoreDataManager storeDataManager )
    {
        this.storeDataManager = storeDataManager;
    }

    @Override
    public SiteConfig lookup( String siteId )
    {
        StoreKey key = StoreKey.fromString( siteId );
        try
        {
            final RemoteRepository repository = storeDataManager.getRemoteRepository( key.getName() );
            return toSiteConfig( repository );
        }
        catch ( AproxDataException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to retrieve ArtifactStore for site key: " + key, e );
        }

        return null;
    }

    public SiteConfig toSiteConfig( RemoteRepository repository )
    {
        SiteConfigBuilder builder = new SiteConfigBuilder( repository.getName(), repository.getUrl() );

        builder.withKeyCertPem( repository.getKeyCertPem() )
               .withProxyHost( repository.getProxyHost() )
               .withProxyPort( repository.getProxyPort() )
               .withProxyUser( repository.getProxyUser() )
               .withRequestTimeoutSeconds( repository.getTimeoutSeconds() )
               .withServerCertPem( repository.getServerCertPem() )
               .withTrustType( SiteTrustType.TRUST_SELF_SIGNED )
               .withUser( repository.getUser() );

        SiteConfig config = builder.build();
        config.setAttribute( PASSWORD_PREFIX + KEY.name(),
                             repository.getKeyPassword() );

        config.setAttribute( PASSWORD_PREFIX + USER.name(),
                             repository.getPassword() );

        config.setAttribute( PASSWORD_PREFIX + PROXY.name(),
                             repository.getProxyPassword() );

        return config;
    }

}
