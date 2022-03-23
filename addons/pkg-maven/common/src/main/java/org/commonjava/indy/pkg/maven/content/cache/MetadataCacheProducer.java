/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.pkg.maven.content.cache;

import org.commonjava.indy.pkg.maven.content.MetadataKey;
import org.commonjava.indy.pkg.maven.content.MetadataInfo;
import org.commonjava.indy.pkg.maven.content.MetadataKeyTransformer;
import org.commonjava.indy.pkg.maven.content.marshaller.MetadataInfoMarshaller;
import org.commonjava.indy.pkg.maven.content.marshaller.MetadataKeyMarshaller;
import org.commonjava.indy.pkg.maven.content.marshaller.MetadataMarshaller;
import org.commonjava.indy.pkg.maven.content.marshaller.SnapshotMarshaller;
import org.commonjava.indy.pkg.maven.content.marshaller.SnapshotVersionMarshaller;
import org.commonjava.indy.pkg.maven.content.marshaller.StoreKeyMarshaller;
import org.commonjava.indy.pkg.maven.content.marshaller.StoreTypeMarshaller;
import org.commonjava.indy.pkg.maven.content.marshaller.VersioningMarshaller;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.infinispan.config.ISPNRemoteConfiguration;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.query.Search;
import org.infinispan.query.spi.SearchManagerImplementor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MetadataCacheProducer
{
    private static final String MAVEN_METADATA_KEY_CACHE = "maven-metadata-key-cache";

    private static final String MAVEN_METADATA_CACHE = "maven-metadata-cache";

    @Inject
    private MavenMetadataCacheListener cacheListener;

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private ISPNRemoteConfiguration remoteConfiguration;

    @MavenMetadataCache
    @Produces
    @ApplicationScoped
    public BasicCacheHandle<MetadataKey, MetadataInfo> mavenMetadataCacheCfg()
    {
        if ( remoteConfiguration.isEnabled() )
        {
            List<BaseMarshaller> infoMarshallers = new ArrayList<>();
            infoMarshallers.add( new MetadataInfoMarshaller() );
            infoMarshallers.add( new MetadataMarshaller() );
            infoMarshallers.add( new VersioningMarshaller() );
            infoMarshallers.add( new SnapshotMarshaller() );
            infoMarshallers.add( new SnapshotVersionMarshaller() );
            infoMarshallers.add( new VersioningMarshaller() );
            cacheProducer.registerProtoAndMarshallers( "metadata_info.proto", infoMarshallers );

            List<BaseMarshaller> keyMarshallers = new ArrayList<>();
            keyMarshallers.add( new MetadataKeyMarshaller() );
            keyMarshallers.add( new StoreKeyMarshaller() );
            keyMarshallers.add( new StoreTypeMarshaller() );
            cacheProducer.registerProtoAndMarshallers( "metadata_key.proto", keyMarshallers );
        }
        return cacheProducer.getBasicCache(MAVEN_METADATA_CACHE);
    }

    @MavenMetadataKeyCache
    @Produces
    @ApplicationScoped
    public BasicCacheHandle<MetadataKey, MetadataKey> mavenMetadataKeyCacheCfg()
    {
        if ( remoteConfiguration.isEnabled() )
        {
            List<BaseMarshaller> keyMarshallers = new ArrayList<>();
            keyMarshallers.add( new MetadataKeyMarshaller() );
            keyMarshallers.add( new StoreKeyMarshaller() );
            keyMarshallers.add( new StoreTypeMarshaller() );
            cacheProducer.registerProtoAndMarshallers( "metadata_key.proto", keyMarshallers );
        }

        BasicCacheHandle<MetadataKey, MetadataKey> handler = cacheProducer.getBasicCache(MAVEN_METADATA_KEY_CACHE);

        registerTransformer( handler );

        return handler;
    }

    private void registerTransformer( BasicCacheHandle handler )
    {
        // for embedded mode
        if ( handler instanceof CacheHandle )
        {
            ((CacheHandle<MetadataKey, MetadataKey>) handler).executeCache( cache -> {
                SearchManagerImplementor searchManager = (SearchManagerImplementor) Search.getSearchManager( cache );
                searchManager.registerKeyTransformer( MetadataKey.class, MetadataKeyTransformer.class );
                return null;
            } );
        }

        if ( handler.getCache() instanceof RemoteCache )
        {
            ((RemoteCache)handler.getCache()).addClientListener( cacheListener );
        }
        else
        {
            ((Cache)handler.getCache()).addListener( cacheListener );
        }
    }

}
