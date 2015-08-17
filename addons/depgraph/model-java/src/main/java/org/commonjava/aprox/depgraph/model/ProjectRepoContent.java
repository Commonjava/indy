package org.commonjava.aprox.depgraph.model;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class ProjectRepoContent
    implements Iterable<ArtifactRepoContent>
{
    private Set<ArtifactRepoContent> artifacts;

    public ProjectRepoContent(){}

    public synchronized void addArtifact(ArtifactRepoContent artifact )
    {
        if ( artifacts == null )
        {
            artifacts = new HashSet<>();
        }

        this.artifacts.add(artifact);
    }

    public Set<ArtifactRepoContent> getArtifacts()
    {
        return artifacts;
    }

    public void setArtifacts( Set<ArtifactRepoContent> artifacts )
    {
        this.artifacts = artifacts;
    }

    @Override
    public Iterator<ArtifactRepoContent> iterator()
    {
        return artifacts == null ? Collections.<ArtifactRepoContent> emptySet().iterator() : artifacts.iterator();
    }
}
