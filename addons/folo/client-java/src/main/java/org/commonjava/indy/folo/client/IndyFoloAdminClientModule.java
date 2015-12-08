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
package org.commonjava.indy.folo.client;

import org.apache.http.HttpStatus;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.IndyResponseErrorDetails;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.folo.dto.TrackedContentDTO;
import org.commonjava.indy.folo.model.TrackedContentRecord;
import org.commonjava.indy.model.core.StoreType;

import java.io.IOException;
import java.io.InputStream;

public class IndyFoloAdminClientModule
    extends IndyClientModule
{

    public boolean initReport( final String trackingId )
        throws IndyClientException
    {
        return http.put( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ), trackingId );
    }

    public InputStream getTrackingRepoZip( String trackingId )
            throws IndyClientException, IOException
    {
        HttpResources resources = getHttp().getRaw( UrlUtils.buildUrl("folo/admin", trackingId, "repo/zip" ) );
        if ( resources.getStatusCode() != HttpStatus.SC_OK )
        {
            throw new IndyClientException( resources.getStatusCode(), "Error retrieving repository zip for tracking record: %s.\n%s",
                                            trackingId, new IndyResponseErrorDetails( resources.getResponse() ) );
        }

        return resources.getResponseEntityContent();
    }

    public TrackedContentDTO getTrackingReport( final String trackingId )
        throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "report" ), TrackedContentDTO.class );
    }

    public TrackedContentRecord getRawTrackingRecord( final String trackingId )
        throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ), TrackedContentRecord.class );
    }

    public void clearTrackingRecord( final String trackingId )
        throws IndyClientException
    {
        http.delete( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ) );
    }

    @Deprecated
    public TrackedContentDTO getTrackingReport( final String trackingId, final StoreType type, final String name )
        throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "report" ),
                         TrackedContentDTO.class );
    }

    @Deprecated
    public TrackedContentRecord getRawTrackingRecord( final String trackingId, final StoreType type, final String name )
        throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ),
                         TrackedContentRecord.class );
    }

    @Deprecated
    public void clearTrackingRecord( final String trackingId, final StoreType type, final String name )
        throws IndyClientException
    {
        http.delete( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ) );
    }

}
