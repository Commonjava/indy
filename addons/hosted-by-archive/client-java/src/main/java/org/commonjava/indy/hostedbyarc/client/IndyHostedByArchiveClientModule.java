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
package org.commonjava.indy.hostedbyarc.client;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.HostedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.commonjava.indy.client.core.helper.HttpResources.cleanupResources;
import static org.commonjava.indy.client.core.helper.HttpResources.entityToString;
import static org.apache.http.HttpStatus.*;

public class IndyHostedByArchiveClientModule
        extends IndyClientModule
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final ContentType CONTENT_TYPE_ZIP = ContentType.create( "application/zip" );

    public static final String HOSTED_BY_ARC_PATH = "admin/stores/maven/hosted";

    public HostedRepository createRepo( final File zipFile, final String repoName, final String ignorePathPrefix )
            throws IndyClientException
    {
        final String endPath = StringUtils.isBlank( ignorePathPrefix ) ?
                "compressed-content" :
                "compressed-content?pathPrefixToIgnore=" + ignorePathPrefix;
        final String urlPath = UrlUtils.buildUrl( http.getBaseUrl(), HOSTED_BY_ARC_PATH, repoName, endPath );

        HttpPost postRequest = new HttpPost( urlPath );
        HttpResources resources = null;

        try
        {
            postRequest.setHeader( "Content-Type", CONTENT_TYPE_ZIP.getMimeType() );
            InputStreamEntity entity = new InputStreamEntity( new FileInputStream( zipFile ), CONTENT_TYPE_ZIP );
            postRequest.setEntity( entity );

            resources = http.execute( postRequest );

            if ( resources != null )
            {
                HttpResponse response = resources.getResponse();
                final StatusLine sl = response.getStatusLine();

                if ( sl.getStatusCode() != SC_OK && sl.getStatusCode() != SC_CREATED )
                {
                    if ( sl.getStatusCode() == SC_NOT_FOUND )
                    {
                        return null;
                    }

                    throw new IndyClientException( sl.getStatusCode(), "Error create %s with file: %s", repoName,
                                                   zipFile.getName() );
                }

                final String json = entityToString( response );
                logger.debug( "Got JSON:\n\n{}\n\n", json );
                final HostedRepository value = http.getObjectMapper().readValue( json, HostedRepository.class );

                logger.debug( "Got result object: {}", value );

                return value;
            }
            return null;
        }
        catch ( IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e );
        }
        finally
        {
            if ( resources != null )
            {
                cleanupResources( postRequest, resources.getResponse(), (CloseableHttpClient) resources.getClient() );
            }
        }

    }

    public HostedRepository createRepo( final File zipFile, final String repoName )
            throws IndyClientException
    {
        return createRepo( zipFile, repoName, "" );
    }

}
