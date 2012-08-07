package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.core.model.DeployPoint;

public class JsonDeployPointExternalizer
    extends JsonExternalizer<DeployPoint>
{

    private static final long serialVersionUID = 1L;

    public JsonDeployPointExternalizer()
    {
        super( DeployPoint.class );
    }

}
