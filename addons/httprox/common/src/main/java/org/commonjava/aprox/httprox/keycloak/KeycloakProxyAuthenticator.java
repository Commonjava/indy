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
package org.commonjava.aprox.httprox.keycloak;

import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.aprox.subsys.http.HttpWrapper;
import org.commonjava.aprox.subsys.http.util.UserPass;
import org.commonjava.aprox.subsys.keycloak.KeycloakAuthenticator;
import org.commonjava.aprox.subsys.keycloak.conf.KeycloakConfig;
import org.commonjava.aprox.util.ApplicationStatus;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by jdcasey on 9/1/15.
 */
@ApplicationScoped
public class KeycloakProxyAuthenticator
        implements KeycloakAuthenticator
{

    @Inject
    private HttproxConfig httproxConfig;

    @Inject
    private KeycloakConfig keycloakConfig;

    private KeycloakDeployment deployment;

    protected KeycloakProxyAuthenticator(){}

    public KeycloakProxyAuthenticator( KeycloakConfig config, HttproxConfig httproxConfig )
    {
        this.keycloakConfig = config;
        this.httproxConfig = httproxConfig;
    }

    @Override
    public boolean authenticate( UserPass userPass, HttpWrapper http )
            throws IOException
    {
        if ( !keycloakConfig.isEnabled() )
        {
            return true;
        }

        synchronized ( this )
        {
            if ( deployment == null )
            {
                String jsonPath = keycloakConfig.getKeycloakJson();
                File jsonFile = new File( jsonPath );
                if ( !jsonFile.exists() )
                {
                    try (FileInputStream in = new FileInputStream( jsonFile ))
                    {
                        deployment = KeycloakDeploymentBuilder.build( in );
                    }
                }
            }
        }

        List<String> authHeaders = http.getHeaders( "Authorization" );
        if (authHeaders == null || authHeaders.size() == 0) {
            sendChallengeResponse(http, null, null);
            return false;
        }

        String tokenString = null;
        for (String authHeader : authHeaders) {
            String[] split = authHeader.trim().split("\\s+");
            if (split.length != 2) continue;
            if (!split[0].equalsIgnoreCase("Bearer")) continue;
            tokenString = split[1];
        }

        if (tokenString == null) {
            sendChallengeResponse( http, null, null );
            return false;
        }

        return (authenticateToken(http, tokenString));
    }

    protected boolean authenticateToken(HttpWrapper exchange, String tokenString)
            throws IOException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        AccessToken token;
        try {
            token = RSATokenVerifier.verifyToken( tokenString, deployment.getRealmKey(), deployment.getRealmInfoUrl() );
        } catch (VerificationException e) {
            logger.error("Failed to verify token", e);
            sendChallengeResponse( exchange, "invalid_token", e.getMessage() );
            return false;
        }
        if (token.getIssuedAt() < deployment.getNotBefore()) {
            logger.error("Stale token");
            sendChallengeResponse( exchange, "invalid_token", "Stale token" );
            return false;
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

        return true;
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


    protected void sendChallengeResponse(HttpWrapper http, String error, String description)
            throws IOException
    {
        StringBuilder header = new StringBuilder("Bearer realm=\"");
        header.append(httproxConfig.getProxyRealm()).append("\"");
        if (error != null) {
            header.append(", error=\"").append(error).append("\"");
        }
        if (description != null) {
            header.append(", error_description=\"").append(description).append("\"");
        }
        final String challenge = header.toString();

        ApplicationStatus stat = ApplicationStatus.UNAUTHORIZED;
        http.writeStatus( stat.code(), stat.message() );
        http.writeHeader( "WWW-Authenticate", challenge );
    }
}
