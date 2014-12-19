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

    public TrackedContentDTO getTrackingReport( final String trackingId, final StoreType type, final String name )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "report", type.singularEndpointName(), name ),
                         TrackedContentDTO.class );
    }

    public TrackedContentRecord getRawTrackingRecord( final String trackingId, final StoreType type, final String name )
        throws AproxClientException
    {
        return http.get( UrlUtils.buildUrl( "/folo/admin", trackingId, "record", type.singularEndpointName(), name ),
                         TrackedContentRecord.class );
    }

    public void clearTrackingRecord( final String trackingId, final StoreType type, final String name )
        throws AproxClientException
    {
        http.delete( UrlUtils.buildUrl( "/folo/admin", trackingId, "record", type.singularEndpointName(), name ) );
    }

}
