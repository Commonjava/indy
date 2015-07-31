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
package org.commonjava.aprox.folo.client;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.folo.dto.TrackedContentDTO;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.model.core.StoreType;

public class AproxFoloAdminClientModule
    extends AproxClientModule
{

    public boolean initReport( final String trackingId )
        throws AproxClientException
    {
        return http.put( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ), trackingId );
    }

    public TrackedContentDTO getTrackingReport( final String trackingId )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "report" ), TrackedContentDTO.class );
    }

    public TrackedContentRecord getRawTrackingRecord( final String trackingId )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ), TrackedContentRecord.class );
    }

    public void clearTrackingRecord( final String trackingId )
        throws AproxClientException
    {
        http.delete( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ) );
    }

    @Deprecated
    public TrackedContentDTO getTrackingReport( final String trackingId, final StoreType type, final String name )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "report" ),
                         TrackedContentDTO.class );
    }

    @Deprecated
    public TrackedContentRecord getRawTrackingRecord( final String trackingId, final StoreType type, final String name )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ),
                         TrackedContentRecord.class );
    }

    @Deprecated
    public void clearTrackingRecord( final String trackingId, final StoreType type, final String name )
        throws AproxClientException
    {
        http.delete( UrlUtils.buildUrl( "/folo/admin", trackingId, "record" ) );
    }

}
