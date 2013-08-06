package org.commonjava.aprox.depgraph.maven;

import javax.enterprise.context.ApplicationScoped;

import org.apache.maven.model.building.ModelCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@ApplicationScoped
public class DepgraphModelCache
    implements ModelCache
{

    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
                                                            .concurrencyLevel( 10 )
                                                            .maximumSize( 10000 )
                                                            .weakValues()
                                                            .weakKeys()
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
