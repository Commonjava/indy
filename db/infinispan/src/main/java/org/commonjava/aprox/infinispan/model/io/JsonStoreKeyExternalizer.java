package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.aprox.subsys.infinispan.io.JsonExternalizer;
import org.commonjava.web.json.ser.JsonSerializer;

public class JsonStoreKeyExternalizer
    extends JsonExternalizer<StoreKey>
{

    private static final long serialVersionUID = 1L;

    public JsonStoreKeyExternalizer()
    {
        super( StoreKey.class, new JsonSerializer( new StoreKeySerializer() ) );
    }

}
