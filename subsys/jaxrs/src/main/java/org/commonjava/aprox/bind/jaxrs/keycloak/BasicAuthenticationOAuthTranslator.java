package org.commonjava.aprox.bind.jaxrs.keycloak;

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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.commonjava.aprox.keycloak.conf.KeycloakConfig;
import org.commonjava.aprox.subsys.http.util.UserPass;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BasicAuthenticationOAuthTranslator
    implements AuthenticationMechanism
{

    private static final String USERNAME = "username";

    private static final String PASSWORD = "password";

    private static final String BEARER_AUTH_PREFIX = "bearer";

    private static final String BASIC_AUTH_PREFIX = "basic";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String CLIENT_ID = "client_id";

    private static final String GRANT_TYPE = "grant_type";

    private static final String PASSWORD_GRANT_TYPE = "password";

    // this URL is based on:
    // https://docs.jboss.org/keycloak/docs/1.2.0.CR1/userguide/html_single/index.html#direct-access-grants
    // If this becomes deprecated, that's the chapter to look in for the updated URL.
    private static final String TOKEN_PATH = "/realms/{realm-name}/protocol/openid-connect/token";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private KeycloakConfig config;

    @Inject
    private Http http;

    protected BasicAuthenticationOAuthTranslator()
    {
    }

    public BasicAuthenticationOAuthTranslator( final KeycloakConfig config, final Http http )
    {
        this.config = config;
        this.http = http;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate( final HttpServerExchange exchange,
                                                        final SecurityContext securityContext )
    {
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
                    logger.info( "BASIC authentication translated into OAuth 2.0 bearer token. Handing off to Keycloak." );
                    resultValues.add( value );
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
                                          .path( TOKEN_PATH )
                                          .build( config.getRealm() );

        logger.debug( "Looking up token at: {}", uri );
        final HttpPost request = new HttpPost( uri );

        final List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add( new BasicNameValuePair( USERNAME, userPass.getUser() ) );
        params.add( new BasicNameValuePair( PASSWORD, userPass.getPassword() ) );
        params.add( new BasicNameValuePair( CLIENT_ID, config.getServerResource() ) );
        params.add( new BasicNameValuePair( GRANT_TYPE, PASSWORD_GRANT_TYPE ) );

        final String authorization =
            BasicAuthHelper.createHeader( config.getServerResource(), config.getServerCredentialSecret() );

        request.setHeader( AUTHORIZATION_HEADER, authorization );

        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;

        AccessTokenResponse tokenResponse = null;
        try
        {
            client = http.createClient();

            final UrlEncodedFormEntity form = new UrlEncodedFormEntity( params, "UTF-8" );
            request.setEntity( form );

            response = client.execute( request );

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
        catch ( final IOException e )
        {
            logger.error( String.format( "Keycloak token request failed: %s", e.getMessage() ), e );
            http.cleanup( client, request, response );
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
