package org.commonjava.indy.bind.jaxrs.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.commonjava.indy.bind.jaxrs.keycloak.AuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class JwtTokenUtils
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );
    
    private static final String DEFAULT_SUBJECT = "sub";

    @Inject
    private AuthConfig authConfig;

    public String generateToken()
    {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken( claims );
    }

    private synchronized String doGenerateToken( Map<String, Object> claims ) {

        long currentTimeMillis = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims( claims )
                .setSubject( DEFAULT_SUBJECT )
                .setIssuedAt( new Date( currentTimeMillis ) )
                .setExpiration( new Date( currentTimeMillis + authConfig.getTokenExpirationHours() * 1000 ) )
                //Sign the JWT using the HS512 algorithm and secret key.
                .signWith( SignatureAlgorithm.HS512, authConfig.getSecret().getBytes())
                .compact();
    }

    public Boolean isExpired( String token )
    {
        Boolean tokenExpired = Boolean.TRUE;
        try
        {
            Claims claims = Jwts.parser().setSigningKey( authConfig.getSecret().getBytes() )
                    .parseClaimsJws( token ).getBody();
            tokenExpired = claims.getExpiration().before( new Date() );
        }
        catch ( ExpiredJwtException ex )
        {
            DefaultClaims claims = (DefaultClaims) ex.getClaims();
            tokenExpired = claims.getExpiration().before( new Date() );
        }
        catch ( Exception e )
        {
            logger.warn( "Validate token failed: {}", e.getMessage() );
        }

        return tokenExpired;
    }

}
