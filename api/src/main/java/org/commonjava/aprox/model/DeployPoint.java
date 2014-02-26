/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.model;

import com.google.gson.annotations.SerializedName;
import com.wordnik.swagger.annotations.ApiClass;

@ApiClass( description = "Representation of an artifact store to which artifacts may be deployed. These are locally hosted.", value = "Local deployment storage (deploy-point)" )
public class DeployPoint
    extends ArtifactStore
{

    private static final long serialVersionUID = 1L;

    @SerializedName( "allow_snapshots" )
    private boolean allowSnapshots = false;

    @SerializedName( "allow_releases" )
    private boolean allowReleases = true;

    private int snapshotTimeoutSeconds;

    public DeployPoint()
    {
        super( StoreType.deploy_point );
    }

    public DeployPoint( final String name )
    {
        super( StoreType.deploy_point, name );
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
        return String.format( "DeployPoint [allowSnapshots={}, allowReleases={}, getName()={}, getKey()={}]",
                              allowSnapshots, allowReleases, getName(), getKey() );
    }

    public int getSnapshotTimeoutSeconds()
    {
        return snapshotTimeoutSeconds;
    }

    public void setSnapshotTimeoutSeconds( final int snapshotTimeoutSeconds )
    {
        this.snapshotTimeoutSeconds = snapshotTimeoutSeconds;
    }

}
