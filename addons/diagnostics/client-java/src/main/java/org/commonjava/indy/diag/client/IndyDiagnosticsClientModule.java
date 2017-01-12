package org.commonjava.indy.diag.client;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.IndyResponseErrorDetails;
import org.commonjava.indy.client.core.helper.HttpResources;

import java.io.IOException;
import java.util.zip.ZipInputStream;

import static org.commonjava.indy.client.core.util.UrlUtils.buildUrl;

/**
 * Created by jdcasey on 1/11/17.
 */
public class IndyDiagnosticsClientModule
        extends IndyClientModule
{

    public ZipInputStream getDiagnosticBundle()
            throws IndyClientException
    {
        HttpGet get = new HttpGet( buildUrl( getHttp().getBaseUrl(), "/diag/bundle" ) );
        HttpResources resources = getHttp().getRaw( get );

        HttpResponse response = resources.getResponse();
        StatusLine sl = response.getStatusLine();
        if ( sl.getStatusCode() != 200 )
        {
            throw new IndyClientException( sl.getStatusCode(), "Error retrieving diagnostic bundle: %s",
                                           new IndyResponseErrorDetails( response ) );
        }

        try
        {
            return new ZipInputStream( resources.getResponseStream() );
        }
        catch ( IOException e )
        {
            throw new IndyClientException( "Failed to read bundle stream from response: %s", e, new IndyResponseErrorDetails( response ) );
        }
    }
}
