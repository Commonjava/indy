package org.commonjava.indy.promote.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.promote.model.AbstractPromoteResult;
import org.commonjava.indy.promote.model.CallbackTarget;
import org.commonjava.indy.subsys.http.util.IndySiteConfigLookup;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.auth.ClientAuthenticator;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.io.IOException;
import java.util.Map;

import static org.apache.http.client.utils.HttpClientUtils.closeQuietly;
import static org.commonjava.indy.promote.model.CallbackTarget.CallbackMethod.POST;
import static org.commonjava.indy.promote.model.CallbackTarget.CallbackMethod.PUT;
import static org.commonjava.indy.util.ApplicationStatus.OK;

/**
 * Created by ruhan on 12/10/18.
 */
@ApplicationScoped
public class PromotionCallbackHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String PROMOTION_CALLBACK_SITE_ID = "promotion_callback"; // set it in http.conf

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private IndySiteConfigLookup siteConfigLookup;

    private SiteConfig config;

    private HttpFactory httpFactory;

    public PromotionCallbackHelper()
    {
        this( new SiteConfigBuilder().withRequestTimeoutSeconds( 30 ).build(), new ObjectMapper() );
    }

    public PromotionCallbackHelper( SiteConfig config, ObjectMapper objectMapper )
    {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    private void init()
    {
        this.config = siteConfigLookup.lookup( PROMOTION_CALLBACK_SITE_ID );
        ClientAuthenticator authenticator = null;
        this.httpFactory = new HttpFactory( authenticator );
    }

    public <T extends AbstractPromoteResult> T callback( CallbackTarget target, T ret )
    {
        CloseableHttpClient client = null;
        try
        {
            boolean isCallbackSuccess;
            client = httpFactory.createClient( config );
            if ( target.getMethod() == POST )
            {
                HttpPost req = new HttpPost( target.getUrl() );
                isCallbackSuccess = execute( client, req, target, ret );
            }
            else if ( target.getMethod() == PUT )
            {
                HttpPut req = new HttpPut( target.getUrl() );
                isCallbackSuccess = execute( client, req, target, ret );
            }
            else
            {
                throw new IllegalArgumentException( target.getMethod() + " not supported" );
            }

            if ( !isCallbackSuccess )
            {
                logger.warn( "Callback failed and stash it, target: {}", target );
                stash( target, ret );
            }
        }
        catch ( Exception e )
        {
            logger.error( "Callback failed", e );
            stash( target, ret );
        }
        finally
        {
            closeQuietly( client );
        }
        return ret;
    }

    // TODO: add to queue and try later
    private <T extends AbstractPromoteResult> void stash( CallbackTarget target, T ret )
    {
    }

    private <T extends AbstractPromoteResult> boolean execute( CloseableHttpClient client, HttpEntityEnclosingRequestBase req, CallbackTarget target, T ret )
                    throws IOException
    {
        addHeadersAndSetEntity( req, target, ret );
        CloseableHttpResponse response = client.execute( req );
        return isCallbackOk( response.getStatusLine().getStatusCode() );
    }

    private <T extends AbstractPromoteResult> void addHeadersAndSetEntity( HttpEntityEnclosingRequestBase req, CallbackTarget target, T ret )
                    throws IOException
    {
        Map<String, String> headers = target.getHeaders();
        if ( headers != null )
        {
            for ( String key : headers.keySet() )
            {
                req.setHeader( key, headers.get( key ) );
            }
        }
        req.addHeader( "Authorization", "Bearer " + target.getAuthToken() );
        req.addHeader( "Content-Type", "application/json" );
        req.setEntity( new StringEntity( objectMapper.writeValueAsString( ret ) ) );
    }

    private boolean isCallbackOk( int statusCode )
    {
        return statusCode == OK.code();
    }

}
