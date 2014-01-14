package org.commonjava.aprox.bind.jaxrs.util;

import static org.commonjava.web.json.ser.ServletSerializerUtils.fromRequestBody;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class ModelServletUtils
{

    @Inject
    @AproxData
    private JsonSerializer restSerializer;

    public ArtifactStore storeFromRequestBody( final StoreType st, final HttpServletRequest request )
    {
        switch ( st )
        {
            case deploy_point:
            {
                return fromRequestBody( request, restSerializer, DeployPoint.class );
            }
            case group:
            {
                return fromRequestBody( request, restSerializer, Group.class );
            }
            default:
            {
                return fromRequestBody( request, restSerializer, Repository.class );
            }
        }
    }

}
