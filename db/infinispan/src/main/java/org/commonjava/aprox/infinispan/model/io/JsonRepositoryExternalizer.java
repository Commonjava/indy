package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.aprox.subsys.infinispan.io.JsonExternalizer;
import org.commonjava.web.json.ser.JsonSerializer;

public class JsonRepositoryExternalizer
    extends JsonExternalizer<Repository>
{

    private static final long serialVersionUID = 1L;

    public JsonRepositoryExternalizer()
    {
        super( Repository.class, new JsonSerializer( new StoreKeySerializer() ) );
    }

}
