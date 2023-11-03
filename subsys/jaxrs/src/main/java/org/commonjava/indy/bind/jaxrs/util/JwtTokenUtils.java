/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.commonjava.indy.bind.jaxrs.keycloak.AuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;
import java.util.Map;

@ApplicationScoped
public class JwtTokenUtils
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );
    
    private static final String DEFAULT_SUBJECT = "sub";

    private static final String CLAIM_BUILD_ID = "build-id";

    @Inject
    private AuthConfig authConfig;

    public JwtTokenUtils() {}

    public JwtTokenUtils(AuthConfig authConfig)
    {
        this.authConfig = authConfig;
    }

    public String generateToken( String buildId )
    {
        Map<String, Object> claims = Jwts.claims();
        claims.put( CLAIM_BUILD_ID, buildId );
        return doGenerateToken( claims );
    }

    private synchronized String doGenerateToken( Map<String, Object> claims ) {

        long currentTimeMillis = System.currentTimeMillis();

        logger.info( "Generate token with claims: {}", claims );

        return Jwts.builder()
                .setClaims( claims )
                .setSubject( DEFAULT_SUBJECT )
                .setIssuedAt( new Date( currentTimeMillis ) )
                .setExpiration( new Date( currentTimeMillis + authConfig.getTokenExpirationHours() * 60 * 60 * 1000 ) )
                //Sign the JWT using the HS512 algorithm and secret key.
                .signWith( SignatureAlgorithm.HS512, authConfig.getSecret().getBytes())
                .compact();
    }

    public boolean validate( String id, String token )
    {
        try
        {
            Claims claims = Jwts.parser().setSigningKey( authConfig.getSecret().getBytes() )
                    .parseClaimsJws( token ).getBody();

            logger.info("Validation with claims: {}", claims );

            if ( claims.getExpiration().before( new Date() ) )
            {
                logger.warn( "Token with claims {} expired.", claims );
                return false;
            }

            if ( claims.get( CLAIM_BUILD_ID )!= null && id.contains( (String)claims.get( CLAIM_BUILD_ID ) ) )
            {
                return true;
            }
        }
        catch ( ExpiredJwtException ex )
        {
            logger.warn( "Token with claims {} expired: {}", ex.getClaims(), ex.getMessage() );
        }
        catch ( Exception e )
        {
            logger.warn( "Validate token failed: {}", e.getMessage() );
        }

        return false;

    }

}
