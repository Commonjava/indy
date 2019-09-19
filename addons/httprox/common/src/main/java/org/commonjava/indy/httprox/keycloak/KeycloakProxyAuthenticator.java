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
package org.commonjava.indy.httprox.keycloak;

import org.apache.commons.codec.binary.Base64;
import org.commonjava.indy.httprox.conf.HttproxConfig;
import org.commonjava.indy.subsys.http.HttpWrapper;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.subsys.keycloak.KeycloakAuthenticator;
import org.commonjava.indy.subsys.keycloak.conf.KeycloakConfig;
import org.commonjava.indy.subsys.keycloak.util.KeycloakBearerTokenDebug;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.ApplicationStatus;
import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.rotation.HardcodedPublicKeyLocator;
import org.keycloak.adapters.rotation.PublicKeyLocator;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.indy.httprox.util.HttpProxyConstants.PROXY_AUTHENTICATE_FORMAT;


/**
 * Created by jdcasey on 9/1/15.
 * Shamelessly ripped from BearerTokenRequestAuthenticator in keycloak-adapter-api.
 */
@ApplicationScoped
public class KeycloakProxyAuthenticator
        implements KeycloakAuthenticator
{

    private static final String TOKEN_HEADER = "TOKEN";

    @Inject
    private HttproxConfig httproxConfig;

    @Inject
    private KeycloakConfig keycloakConfig;

    private KeycloakDeployment deployment;

    protected KeycloakProxyAuthenticator()
    {
    }

    public KeycloakProxyAuthenticator( KeycloakConfig config, HttproxConfig httproxConfig )
    {
        this.keycloakConfig = config;
        this.httproxConfig = httproxConfig;
    }

    private PublicKey getHardcodedRealmKey( KeycloakDeployment deployment )
    {
        PublicKeyLocator l = deployment.getPublicKeyLocator();
        if ( l instanceof HardcodedPublicKeyLocator )
        {
            return l.getPublicKey( null, null );
        }
        return null;
    }

    @Override
    public boolean authenticate( UserPass userPass, HttpWrapper http )
            throws IOException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( !keycloakConfig.isEnabled() || !httproxConfig.isSecured() )
        {
            logger.debug( "Keycloak httprox translation authenticator is disabled OR httprox is running in unsecured mode. Skipping authentication." );
            return true;
        }

        synchronized ( this )
        {
            if ( deployment == null )
            {
                String jsonPath = keycloakConfig.getKeycloakJson();

                logger.debug( "Reading keycloak deployment info from: {}", jsonPath );

                File jsonFile = new File( jsonPath );
                if ( jsonFile.exists() )
                {
                    try (FileInputStream in = new FileInputStream( jsonFile ))
                    {
                        deployment = KeycloakDeploymentBuilder.build( in );
                        logger.debug( "Got public key: '{}'", getHardcodedRealmKey( deployment ) );
                    }
                }
                else
                {
                    logger.warn( "Cannot read keycloak.json from: {}", jsonPath );
                    return false;
                }
            }
        }

        String tokenString = userPass.getPassword();

        AuthResult result = null;
        if ( tokenString != null )
        {
            result = authenticateToken( http, tokenString );
        }

        if ( result == null || !result.success )
        {
            String ts = null;
            List<String> headers = http.getHeaders( TOKEN_HEADER );
            if ( headers != null && !headers.isEmpty() )
            {
                ts = new String( Base64.decodeBase64( headers.get( 0 ) ) );
                result = authenticateToken( http, ts );
            }
        }

        if ( result == null )
        {
            logger.info( "No keycloak bearer token provided! This must either be in the password of a BASIC "
                                 + "authentication header, or in a separate Base64-encoded header: {}",
                         TOKEN_HEADER );

            sendChallengeResponse( http, null, null );
            result = new AuthResult( false );
        }
        else if (!result.success)
        {
            sendChallengeResponse( http, result.reason, result.description );
        }

        return result.success;
    }

    private static final class AuthResult
    {
        private boolean success;
        private String reason;
        private String description;

        private AuthResult( boolean success, String reason, String description )
        {
            this.success = success;
            this.reason = reason;
            this.description = description;
        }

        private AuthResult( boolean success )
        {
            this.success = success;
        }
    }

    protected AuthResult authenticateToken( HttpWrapper exchange, String tokenString )
            throws IOException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        AccessToken token;
        try
        {
            KeycloakBearerTokenDebug.debugToken( tokenString );

            logger.debug( "Verifying token: '{}'", tokenString );
            token = RSATokenVerifier.verifyToken( tokenString, getHardcodedRealmKey( deployment ), deployment.getRealmInfoUrl() );
        }
        catch ( VerificationException e )
        {
            logger.error( "Failed to verify token", e );
            return new AuthResult( false, "invalid_token", e.getMessage() );
        }
        if ( token.getIssuedAt() < deployment.getNotBefore() )
        {
            logger.error( "Stale token" );
            return new AuthResult( false, "invalid_token", "Stale token" );
        }

        // TODO: Not yet supported.
        //        boolean verifyCaller = false;
        //        if (deployment.isUseResourceRoleMappings()) {
        //            verifyCaller = token.isVerifyCaller(deployment.getResourceName());
        //        } else {
        //            verifyCaller = token.isVerifyCaller();
        //        }
        //
        //        String surrogate = null;
        //        if (verifyCaller) {
        //            if (token.getTrustedCertificates() == null || token.getTrustedCertificates().size() == 0) {
        //                logger.warn("No trusted certificates in token");
        //                sendClientCertChallenge( exchange );
        //                return false;
        //            }
        //
        //            // for now, we just make sure Undertow did two-way SSL
        //            // assume JBoss Web verifies the client cert
        //            X509Certificate[] chain = new X509Certificate[0];
        //            try {
        //                chain = exchange.getCertificateChain();
        //            } catch (Exception ignore) {
        //
        //            }
        //            if (chain == null || chain.length == 0) {
        //                logger.warn("No certificates provided by undertow to verify the caller");
        //                sendClientCertChallenge( exchange );
        //                return false;
        //            }
        //            surrogate = chain[0].getSubjectDN().getName();
        //        }

        logger.debug( "Token verification succeeded!" );
        return new AuthResult( true );
    }

    //    protected void sendClientCertChallenge(HttpWrapper exchange) {
    //        return new AuthChallenge() {
    //            @Override
    //            public boolean errorPage() {
    //                return false;
    //            }
    //
    //            @Override
    //            public boolean challenge(HttpFacade exchange) {
    //                // do the same thing as client cert auth
    //                return false;
    //            }
    //        };
    //    }

    protected void sendChallengeResponse( HttpWrapper http, String error, String description )
            throws IOException
    {
        StringBuilder header = new StringBuilder( String.format( PROXY_AUTHENTICATE_FORMAT,
                                                                 httproxConfig.getProxyRealm() ) );
        if ( error != null )
        {
            header.append( ", error=\"" ).append( error ).append( "\"" );
        }
        if ( description != null )
        {
            header.append( ", error_description=\"" ).append( description ).append( "\"" );
        }

        final String challenge = header.toString();

        ApplicationStatus stat = ApplicationStatus.PROXY_AUTHENTICATION_REQUIRED;
        http.writeStatus( stat.code(), stat.message() );
        http.writeHeader( ApplicationHeader.proxy_authenticate, challenge );
    }
}
