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
