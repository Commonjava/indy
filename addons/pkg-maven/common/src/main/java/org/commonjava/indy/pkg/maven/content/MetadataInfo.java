/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.maven.content;

import org.apache.maven.artifact.repository.metadata.Metadata;

import java.io.Serializable;

/**
 * Used to bind group metadata and its metadata merge info together for caching easily
 */
public class MetadataInfo
        implements Serializable
{
    private Metadata metadata;

    private String metadataMergeInfo;

    public MetadataInfo( final Metadata metadata )
    {
        this.metadata = metadata;
    }

    public Metadata getMetadata()
    {
        return metadata;
    }

    public void setMetadata( Metadata metadata )
    {
        this.metadata = metadata;
    }

    public String getMetadataMergeInfo()
    {
        return metadataMergeInfo;
    }

    public void setMetadataMergeInfo( String metadataMergeInfo )
    {
        this.metadataMergeInfo = metadataMergeInfo;
    }
}
