package org.commonjava.indy.pkg.maven.content;

import org.infinispan.query.Transformer;

public class MetadataKeyTransformer
                implements Transformer
{
    @Override
    public Object fromString( String s )
    {
        return MetadataKey.fromString( s );
    }

    @Override
    public String toString( Object o )
    {
        return o.toString();
    }
}
