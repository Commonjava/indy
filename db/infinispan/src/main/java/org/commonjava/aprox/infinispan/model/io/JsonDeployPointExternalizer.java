package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.aprox.subsys.infinispan.io.JsonExternalizer;
import org.commonjava.web.json.ser.JsonSerializer;

public class JsonDeployPointExternalizer
    extends JsonExternalizer<DeployPoint>
{

    private static final long serialVersionUID = 1L;

    public JsonDeployPointExternalizer()
    {
        super( DeployPoint.class, new JsonSerializer( new StoreKeySerializer() ) );
    }

}
