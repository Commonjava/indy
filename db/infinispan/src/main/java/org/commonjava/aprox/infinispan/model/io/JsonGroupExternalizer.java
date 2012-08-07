package org.commonjava.aprox.infinispan.model.io;

import org.commonjava.aprox.core.model.Group;

public class JsonGroupExternalizer
    extends JsonExternalizer<Group>
{

    private static final long serialVersionUID = 1L;

    public JsonGroupExternalizer()
    {
        super( Group.class );
    }

}
