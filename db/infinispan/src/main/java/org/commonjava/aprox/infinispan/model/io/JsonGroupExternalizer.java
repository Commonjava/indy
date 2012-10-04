package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.aprox.subsys.infinispan.io.JsonExternalizer;
import org.commonjava.web.json.ser.JsonSerializer;

public class JsonGroupExternalizer
    extends JsonExternalizer<Group>
{

    private static final long serialVersionUID = 1L;

    public JsonGroupExternalizer()
    {
        super( Group.class, new JsonSerializer( new StoreKeySerializer() ) );
    }

}
