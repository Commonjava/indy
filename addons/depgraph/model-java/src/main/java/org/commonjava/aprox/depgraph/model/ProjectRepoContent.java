/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
