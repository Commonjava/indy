package org.commonjava.aprox.depgraph.model;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ArtifactRepoContent
{
    private ArtifactRef artifact;

    private StoreKey repoKey;

    private String path;

    public ArtifactRepoContent(){}

    public ArtifactRepoContent( ArtifactRef artifact, StoreKey storeKey, String path )
    {
        this.artifact = artifact;
        this.repoKey = storeKey;
        this.path = path;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public ArtifactRef getArtifact()
    {
        return artifact;
    }

    public void setArtifact( ArtifactRef artifact )
    {
        this.artifact = artifact;
    }

    public StoreKey getRepoKey()
    {
        return repoKey;
    }

    public void setRepoKey( StoreKey repoKey )
    {
        this.repoKey = repoKey;
    }
}
