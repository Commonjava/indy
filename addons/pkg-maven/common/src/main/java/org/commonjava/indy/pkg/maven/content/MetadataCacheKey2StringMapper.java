package org.commonjava.indy.pkg.maven.content;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;

public final class MetadataCacheKey2StringMapper
                implements TwoWayKey2StringMapper
{

    @Override
    public Object getKeyMapping( String s )
    {
        return MetadataCacheKey.fromString( s );
    }

    @Override
    public boolean isSupportedType( Class<?> aClass )
    {
        return aClass == MetadataCacheKey.class;
    }

    @Override
    public String getStringMapping( Object o )
    {
        return o.toString();
    }
}
