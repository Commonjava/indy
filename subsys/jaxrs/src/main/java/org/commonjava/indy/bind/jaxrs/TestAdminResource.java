package org.commonjava.indy.bind.jaxrs;

import org.commonjava.indy.conf.InternalFeatureConfig;
import org.commonjava.o11yphant.metrics.MetricsManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path( "/api/admin/test" )
public class TestAdminResource
                implements IndyResources
{
    @Inject
    private MetricsManager metricsManager;

    @Inject
    private InternalFeatureConfig internalFeatureConfig;

    @Path( "metrics/reset" )
    @POST
    public Response resetMetrics()
    {
        if ( internalFeatureConfig.getTestFeatures() != Boolean.TRUE )
        {
            return Response.notModified().build();
        }

        metricsManager.reset();
        return Response.ok().build();
    }

}
