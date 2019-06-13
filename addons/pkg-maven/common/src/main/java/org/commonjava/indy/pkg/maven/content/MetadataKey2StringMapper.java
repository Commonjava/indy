package org.commonjava.indy.pkg.maven.content;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;

public final class MetadataKey2StringMapper
                implements TwoWayKey2StringMapper
{

    @Override
    public Object getKeyMapping( String s )
    {
        return MetadataKey.fromString( s );
    }

    @Override
    public boolean isSupportedType( Class<?> aClass )
    {
        return aClass == MetadataKey.class;
    }

    @Override
    public String getStringMapping( Object o )
    {
        return o.toString();
    }
}
