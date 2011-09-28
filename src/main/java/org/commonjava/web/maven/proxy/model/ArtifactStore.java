package org.commonjava.web.maven.proxy.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;

public interface ArtifactStore
{

    public enum StoreType
    {
        group, repository, deploy_store;
    }

    public static final class StoreKey
    {
        private final StoreType type;

        private final String name;

        public StoreKey( final StoreType type, final String name )
        {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return namespaceId( type.name(), name );
        }
    }

    StoreKey getKey();

    String getName();

    StoreType getDoctype();

}