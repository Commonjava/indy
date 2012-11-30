package org.commonjava.aprox.tensor.maven;

import javax.enterprise.context.ApplicationScoped;

import org.apache.maven.model.building.ModelCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@ApplicationScoped
public class TensorModelCache
    implements ModelCache
{

    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
                                                            .maximumSize( 100 )
                                                            .weakValues()
                                                            .build();

    @Override
    public void put( final String groupId, final String artifactId, final String version, final String tag,
                     final Object data )
    {
        cache.put( key( groupId, artifactId, version, tag ), data );
    }

    @Override
    public Object get( final String groupId, final String artifactId, final String version, final String tag )
    {
        return cache.asMap()
                    .get( key( groupId, artifactId, version, tag ) );
    }

    private String key( final String groupId, final String artifactId, final String version, final String tag )
    {
        return String.format( "%s:%s:%s:%s", groupId, artifactId, version, tag );
    }

}
