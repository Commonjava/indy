/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.subsys.http.util;

import static org.commonjava.util.jhttpc.auth.AttributePasswordManager.*;
import static org.commonjava.util.jhttpc.auth.PasswordType.*;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.util.jhttpc.auth.AttributePasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Created by jdcasey on 11/9/15.
 */
public class IndySiteConfigLookup
        implements AttributePasswordManager.SiteConfigLookup
{

    @Inject
    private StoreDataManager storeDataManager;

    protected IndySiteConfigLookup()
    {
    }

    public IndySiteConfigLookup( StoreDataManager storeDataManager )
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
        catch ( IndyDataException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to retrieve ArtifactStore for site key: " + key, e );
        }

        return null;
    }

    public SiteConfig toSiteConfig( RemoteRepository repository )
    {
        SiteConfigBuilder builder = new SiteConfigBuilder( repository.getName(), repository.getUrl() );

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Adding server PEM to site config for: {}\n{}", repository.getKey(), repository.getServerCertPem() );

        builder.withKeyCertPem( repository.getKeyCertPem() )
               .withProxyHost( repository.getProxyHost() )
               .withProxyPort( repository.getProxyPort() )
               .withProxyUser( repository.getProxyUser() )
               .withRequestTimeoutSeconds( repository.getTimeoutSeconds() )
               .withServerCertPem( repository.getServerCertPem() )
               .withTrustType( SiteTrustType.TRUST_SELF_SIGNED )
               .withUser( repository.getUser() );

        SiteConfig config = builder.build();
        logger.debug( "Got server PEM in site config:\n{}", config.getServerCertPem() );
        config.setAttribute( PASSWORD_PREFIX + KEY.name(),
                             repository.getKeyPassword() );

        config.setAttribute( PASSWORD_PREFIX + USER.name(),
                             repository.getPassword() );

        config.setAttribute( PASSWORD_PREFIX + PROXY.name(),
                             repository.getProxyPassword() );

        return config;
    }

}
