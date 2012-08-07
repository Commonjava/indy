package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.core.model.StoreKey;

public class JsonStoreKeyExternalizer
    extends JsonExternalizer<StoreKey>
{

    private static final long serialVersionUID = 1L;

    public JsonStoreKeyExternalizer()
    {
        super( StoreKey.class );
    }

}
