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
package org.commonjava.indy.koji.inject;

import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.commonjava.indy.koji.content.KojiTagInfoEntry;
import org.commonjava.indy.pkg.maven.content.marshaller.MetadataMarshaller;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.atlas.maven.ident.ref.ProjectRef;
import org.commonjava.indy.subsys.infinispan.config.ISPNRemoteConfiguration;
import org.infinispan.protostream.BaseMarshaller;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Create ISPN caches necessary to support Koji metadata provider functions.
 */
@ApplicationScoped
public class KojiCacheProducer
{
    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private ISPNRemoteConfiguration remoteConfiguration;

    @KojiMavenVersionMetadataCache
    @Produces
    @ApplicationScoped
    public BasicCacheHandle<ProjectRef, Metadata> versionMetadataCache()
    {
        if ( remoteConfiguration.isEnabled() )
        {
            List<BaseMarshaller> marshallers = new ArrayList<>();
            marshallers.add( new MetadataMarshaller() );
            marshallers.add( new ProjectRefMarshaller() );
            cacheProducer.registerProtoAndMarshallers( "koji_metadata.proto", marshallers );
        }
        return cacheProducer.getBasicCache( "koji-maven-version-metadata" );
    }

    @KojiTagInfoCache
    @Produces
    @ApplicationScoped
    public BasicCacheHandle<Integer, KojiTagInfoEntry> kojiTagInfoCache()
    {
        if ( remoteConfiguration.isEnabled() )
        {
            List<BaseMarshaller> marshallers = new ArrayList<>();
            marshallers.add( new KojiTagInfoMarshaller() );
            marshallers.add( new KojiTagInfoEntryMarshaller() );
            cacheProducer.registerProtoAndMarshallers( "koji_taginfo.proto", marshallers );
        }
        return cacheProducer.getBasicCache( "koji-tags" );
    }
}
