/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.keycloak;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.commonjava.indy.subsys.http.IndyHttpException;
import org.commonjava.indy.subsys.http.IndyHttpProvider;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakConfig;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.subsys.keycloak.util.KeycloakBearerTokenDebug;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
/** <b>FIXME:</b> Setting "enable-basic-auth" in keycloak.json instead of "bearer-only" should enable BOTH basic auth
 * and bearer token auth. However, something about having the keycloak server on a separate domain seems to
 * be causing problems with the state cookie not getting set during the post-sso redirection. This patches
 * that problem by looking for basic auth first, retrieving the corresponding token, and injecting it as a
 * request header.
 * 
 * @author jdcasey
 */
public class BasicAuthenticationOAuthTranslator
    implements AuthenticationMechanism
{
    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String BEARER_AUTH_PREFIX = "bearer";

    private static final String BASIC_AUTH_PREFIX = "basic";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String INDY_BEARER_TOKEN = "Indy-Bearer";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private KeycloakConfig config;

    @Inject
    private IndyHttpProvider http;

    private boolean enabled;

    protected BasicAuthenticationOAuthTranslator()
    {
    }

    public BasicAuthenticationOAuthTranslator( final KeycloakConfig config, final IndyHttpProvider http )
    {
        this.config = config;
        this.http = http;
        init();
    }

    @PostConstruct
    public void init()
    {
        if ( config.getServerCredentialSecret() == null || config.getServerResource() == null )
        {
            logger.warn( "BASIC authentication is disabled; server.resource and/or server.credential.secret are missing from keycloak.conf!" );
        }
        else
        {
            enabled = true;
        }
    }

    @Override
    public AuthenticationMechanismOutcome authenticate( final HttpServerExchange exchange,
                                                        final SecurityContext securityContext )
    {
        if ( !enabled )
        {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        logger.debug( "BASIC authenticate injector checking for " + AUTHORIZATION_HEADER + " header." );
        final HeaderMap headers = exchange.getRequestHeaders();
        final Collection<String> vals = headers.remove( AUTHORIZATION_HEADER );
        String basicAuth = null;
        String bearerAuth = null;
        final List<String> resultValues = new ArrayList<>();
        if ( vals != null )
        {
            for ( final String value : vals )
            {
                logger.debug( "Found Authorization header: '{}'", value );
                if ( value.toLowerCase()
                          .startsWith( BASIC_AUTH_PREFIX ) )
                {
                    logger.debug( "detected basic auth" );
                    basicAuth = value;
                }
                else if ( value.toLowerCase()
                               .startsWith( BEARER_AUTH_PREFIX ) )
                {
                    bearerAuth = value;
                    resultValues.add( value );
                }
                else
                {
                    resultValues.add( value );
                }
            }
        }

        if ( bearerAuth == null && basicAuth != null )
        {
            final UserPass userPass = UserPass.parse( basicAuth );
            logger.debug( "Parsed BASIC authorization: {}", userPass );
            if ( userPass != null )
            {
                final AccessTokenResponse token = lookupToken( userPass );
                if ( token != null )
                {
                    final String encodedToken = token.getToken();
                    logger.debug( "Raw token: {}", encodedToken );

                    final String value = BEARER_AUTH_PREFIX + " " + encodedToken;

                    logger.debug( "Adding {} value: {}", AUTHORIZATION_HEADER, value );
                    logger.info(
                            "BASIC authentication translated into OAuth 2.0 bearer token. Handing off to Keycloak." );
                    resultValues.add( value );

                    KeycloakBearerTokenDebug.debugToken( encodedToken );
                    exchange.getResponseHeaders().add( new HttpString( INDY_BEARER_TOKEN ), encodedToken );
                }
            }
        }

        logger.debug( "Re-adding {} values: {}", AUTHORIZATION_HEADER, resultValues );
        headers.addAll( new HttpString( AUTHORIZATION_HEADER ), resultValues );

        // No matter what, we don't actually attempt authentication here (from the perspective of the app).
        // The best we can do is lookup the token for the given basic auth fields, and inject it for keycloak to use.
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    private AccessTokenResponse lookupToken( final UserPass userPass )
    {
        final URI uri = KeycloakUriBuilder.fromUri( config.getUrl() )
                                          .path( ServiceUrlConstants.TOKEN_PATH )
                                          .build( config.getRealm() );

        logger.debug( "Looking up token at: {}", uri );
        final HttpPost request = new HttpPost( uri );

        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add( new BasicNameValuePair( USERNAME, userPass.getUser() ) );
        params.add( new BasicNameValuePair( PASSWORD, userPass.getPassword() ) );
        params.add( new BasicNameValuePair( OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD ) );

        final String authorization =
            BasicAuthHelper.createHeader( config.getServerResource(), config.getServerCredentialSecret() );

        request.setHeader( AUTHORIZATION_HEADER, authorization );

        CloseableHttpClient client = null;
        AccessTokenResponse tokenResponse = null;
        try
        {
            client = http.createClient( uri.getHost() );

            final UrlEncodedFormEntity form = new UrlEncodedFormEntity( params, "UTF-8" );
            request.setEntity( form );

            CloseableHttpResponse response =  client.execute( request );

            logger.debug( "Got response status: {}", response.getStatusLine() );
            if ( response.getStatusLine()
                         .getStatusCode() == 200 )
            {
                try (InputStream in = response.getEntity()
                                              .getContent())
                {
                    final String json = IOUtils.toString( in );
                    logger.debug( "Token response:\n\n{}\n\n", json );
                    tokenResponse = JsonSerialization.readValue( json, AccessTokenResponse.class );
                }
            }
        }
        catch ( IOException | IndyHttpException e )
        {
            logger.error( String.format( "Keycloak token request failed: %s", e.getMessage() ), e );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }

        return tokenResponse;
    }

    @Override
    public ChallengeResult sendChallenge( final HttpServerExchange exchange, final SecurityContext securityContext )
    {
        logger.debug( "BASIC sendChallenge" );
        exchange.getResponseHeaders()
                .add( new HttpString( "WWW-Authenticate" ), "BASIC realm=\"" + config.getRealm() + "\"" );

        exchange.setResponseCode( 401 );

        return new ChallengeResult( true, 401 );
    }

}
