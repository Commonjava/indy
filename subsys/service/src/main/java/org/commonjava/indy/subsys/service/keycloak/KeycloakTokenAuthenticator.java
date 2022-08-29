/**
 * Copyright (C) 2020 Red Hat, Inc.
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
package org.commonjava.indy.subsys.service.keycloak;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.commonjava.indy.client.core.auth.IndyClientAuthenticator;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class KeycloakTokenAuthenticator
        extends IndyClientAuthenticator
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_FORMAT = "Bearer %s";

    private final String keycloakAuthUrl;

    private final String keycloakAuthRealm;

    private final String keycloakClientId;

    private final String keycloakClientSecret;

    private Configuration config;

    private String cachedToken;

    private Long cachedTokenExpireAtInSecs;

    public KeycloakTokenAuthenticator( String keycloakAuthUrl, String keycloakAuthRealm, String keycloakClientId,
                                       String keycloakClientSecret )
    {
        this.keycloakAuthUrl = keycloakAuthUrl;
        this.keycloakAuthRealm = keycloakAuthRealm;
        this.keycloakClientId = keycloakClientId;
        this.keycloakClientSecret = keycloakClientSecret;
    }

    @Override
    public HttpClientBuilder decorateClientBuilder( HttpClientBuilder builder )
    {
        builder.addInterceptorFirst( (HttpRequestInterceptor) ( httpRequest, httpContext ) -> {
            final Header header = new BasicHeader( AUTHORIZATION_HEADER, String.format( BEARER_FORMAT, getToken() ) );
            httpRequest.addHeader( header );
        } );

        return builder;
    }

    private Boolean shouldRefresh()
    {
        if ( cachedTokenExpireAtInSecs == null )
        {
            return false;
        }
        final long nowSecs = System.currentTimeMillis() / 1000;
        return nowSecs > cachedTokenExpireAtInSecs;
    }

    private Configuration getKeycloakClientCfg()
    {
        if ( config == null )
        {
            config = new Configuration();
            config.setAuthServerUrl( keycloakAuthUrl );
            config.setRealm( keycloakAuthRealm );
            config.setResource( keycloakClientId );
            config.setCredentials( Collections.singletonMap( "secret", keycloakClientSecret ) );
        }
        return config;
    }

    private String getToken()
    {
        if ( StringUtils.isBlank( cachedToken ) || shouldRefresh() )
        {
            AuthzClient authzClient = AuthzClient.create( getKeycloakClientCfg() );
            AccessTokenResponse response = authzClient.obtainAccessToken();
            cachedToken = response.getToken();
            cachedTokenExpireAtInSecs = System.currentTimeMillis() / 1000 + response.getExpiresIn();
            logger.debug( "Got keycloak access token for client: {}, expire in: {}", keycloakClientId,
                          cachedTokenExpireAtInSecs );
        }
        return cachedToken;
    }
}
