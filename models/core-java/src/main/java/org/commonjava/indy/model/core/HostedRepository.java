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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;

import static org.commonjava.indy.model.core.StoreType.hosted;

@ApiModel( description = "Hosts artifact content on the local system", parent = ArtifactStore.class )
public class HostedRepository
    extends AbstractRepository
{

    private static final long serialVersionUID = 1L;

    private String storage;

    private int snapshotTimeoutSeconds;

    // if readonly, default is not
    @ApiModelProperty( required = false, dataType = "boolean", value = "identify if the hoste repo is readonly" )
    private boolean readonly = false;

    HostedRepository()
    {
        super();
    }

    public HostedRepository( final String packageType, final String name )
    {
        super( packageType, hosted, name );
    }

    @Deprecated
    public HostedRepository( final String name )
    {
        super( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, hosted, name );
    }

    @Override
    public String toString()
    {
        return String.format( "HostedRepository [%s]", getName() );
    }

    public int getSnapshotTimeoutSeconds()
    {
        return snapshotTimeoutSeconds;
    }

    public void setSnapshotTimeoutSeconds( final int snapshotTimeoutSeconds )
    {
        this.snapshotTimeoutSeconds = snapshotTimeoutSeconds;
    }

    public String getStorage()
    {
        return storage;
    }

    public void setStorage( final String storage )
    {
        this.storage = storage;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly( boolean readonly )
    {
        this.readonly = readonly;
    }

    @Override
    public boolean isAuthoritativeIndex()
    {
        return super.isAuthoritativeIndex() || this.isReadonly();
    }

    @Override
    public void setAuthoritativeIndex( boolean authoritativeIndex )
    {
        super.setAuthoritativeIndex( authoritativeIndex || this.isReadonly() );
    }

    @Override
    public HostedRepository copyOf()
    {
        return copyOf( getPackageType(), getName() );
    }

    @Override
    public HostedRepository copyOf( final String packageType, final String name )
    {
        HostedRepository repo = new HostedRepository( packageType, name );
        repo.setStorage( getStorage() );
        repo.setSnapshotTimeoutSeconds( getSnapshotTimeoutSeconds() );
        repo.setReadonly( isReadonly() );
        copyRestrictions( repo );
        copyBase( repo );

        return repo;
    }
}
