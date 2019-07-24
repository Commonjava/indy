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

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.indy.client.core.helper.HttpResources.cleanupResources;
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
        HttpResponse response = null;
        try
        {
            response = resources.getResponse();
            StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != 200 )
            {
                closeQuietly( resources );
                throw new IndyClientException( sl.getStatusCode(), "Error retrieving diagnostic bundle: %s",
                                               new IndyResponseErrorDetails( response ) );
            }

            return new ZipInputStream( resources.getResponseStream() );
        }
        catch ( IOException e )
        {
            closeQuietly( resources );
            throw new IndyClientException( "Failed to read bundle stream from response: %s", e, new IndyResponseErrorDetails( response ) );
        }
        catch ( RuntimeException e )
        {
            closeQuietly( resources );
            throw e;
        }
    }
}
