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
package org.commonjava.indy.model.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Set;

public abstract class AbstractRepository
    extends ArtifactStore
        implements Externalizable
{
    private static final int ABSTRACT_REPOSITORY_VERSION = 1;

    @JsonProperty( "allow_snapshots" )
    private boolean allowSnapshots = false;

    @JsonProperty( "allow_releases" )
    private boolean allowReleases = true;


    public AbstractRepository()
    {
        super();
    }

    protected AbstractRepository( final String packageType, final StoreType type, final String name )
    {
        super( packageType, type, name );
    }

    public boolean isAllowSnapshots()
    {
        return allowSnapshots;
    }

    public void setAllowSnapshots( final boolean allowSnapshots )
    {
        this.allowSnapshots = allowSnapshots;
    }

    public boolean isAllowReleases()
    {
        return allowReleases;
    }

    public void setAllowReleases( final boolean allowReleases )
    {
        this.allowReleases = allowReleases;
    }

    protected void copyRestrictions( AbstractRepository repo )
    {
        repo.setAllowReleases( isAllowReleases() );
        repo.setAllowSnapshots( isAllowSnapshots() );
    }

    @Override
    public void writeExternal( final ObjectOutput out )
            throws IOException
    {
        super.writeExternal( out );

        out.writeInt( ABSTRACT_REPOSITORY_VERSION );
        out.writeBoolean( allowReleases );
        out.writeBoolean( allowSnapshots );
    }

    @Override
    public void readExternal( final ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        super.readExternal( in );

        int abstractRepositoryVersion = in.readInt();
        if ( abstractRepositoryVersion > ABSTRACT_REPOSITORY_VERSION )
        {
            throw new IOException( "Cannot deserialize. AbstractRepository version in data stream is: " + abstractRepositoryVersion
                                           + " but this class can only deserialize up to version: " + ABSTRACT_REPOSITORY_VERSION );
        }

        this.allowReleases = in.readBoolean();
        this.allowSnapshots = in.readBoolean();
    }

}
