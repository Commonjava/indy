package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.model.Repository;

public class JsonRepositoryExternalizer
    extends JsonExternalizer<Repository>
{

    private static final long serialVersionUID = 1L;

    public JsonRepositoryExternalizer()
    {
        super( Repository.class );
    }

}
