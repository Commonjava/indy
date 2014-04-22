/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.model;

import java.io.File;

import com.google.gson.annotations.SerializedName;
import com.wordnik.swagger.annotations.ApiClass;

@ApiClass( description = "Representation of an artifact store whose content is hosted locally.", value = "Local repository storage" )
public class HostedRepository
    extends ArtifactStore
{

    private static final long serialVersionUID = 1L;

    private File storage;

    @SerializedName( "allow_snapshots" )
    private boolean allowSnapshots = false;

    @SerializedName( "allow_releases" )
    private boolean allowReleases = true;

    private int snapshotTimeoutSeconds;

    HostedRepository()
    {
    }

    public HostedRepository( final String name )
    {
        super( name );
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

    @Override
    public String toString()
    {
        return String.format( "HostedRepository [allowSnapshots=%s, allowReleases=%s, getName()=%s, getKey()=%s]", allowSnapshots, allowReleases,
                              getName(), getKey() );
    }

    public int getSnapshotTimeoutSeconds()
    {
        return snapshotTimeoutSeconds;
    }

    public void setSnapshotTimeoutSeconds( final int snapshotTimeoutSeconds )
    {
        this.snapshotTimeoutSeconds = snapshotTimeoutSeconds;
    }

    public File getStorage()
    {
        return storage;
    }

    public void setStorage( final File storage )
    {
        this.storage = storage;
    }

    @Override
    protected StoreKey initKey( final String name )
    {
        return new StoreKey( StoreType.hosted, name );
    }

}
