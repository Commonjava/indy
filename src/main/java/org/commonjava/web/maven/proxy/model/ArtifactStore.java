package org.commonjava.web.maven.proxy.model;

public interface ArtifactStore
{

    public enum StoreType
    {
        group, repository, deploy_store;
    }

    String getName();

    StoreType getDoctype();

}