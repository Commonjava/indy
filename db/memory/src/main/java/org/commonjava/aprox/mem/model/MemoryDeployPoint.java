/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.mem.model;

import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.StoreType;

import com.google.gson.annotations.SerializedName;

public class MemoryDeployPoint
    extends AbstractArtifactStore
    implements DeployPoint
{

    @SerializedName( "allow_snapshots" )
    private boolean allowSnapshots = false;

    @SerializedName( "allow_releases" )
    private boolean allowReleases = true;

    public MemoryDeployPoint()
    {
        super( StoreType.deploy_point );
    }

    public MemoryDeployPoint( final String name )
    {
        super( StoreType.deploy_point, name );
    }

    @Override
    public boolean isAllowSnapshots()
    {
        return allowSnapshots;
    }

    @Override
    public void setAllowSnapshots( final boolean allowSnapshots )
    {
        this.allowSnapshots = allowSnapshots;
    }

    @Override
    public boolean isAllowReleases()
    {
        return allowReleases;
    }

    @Override
    public void setAllowReleases( final boolean allowReleases )
    {
        this.allowReleases = allowReleases;
    }

    @Override
    public String toString()
    {
        return String.format( "MemoryDeployPoint [allowSnapshots=%s, allowReleases=%s, getName()=%s, getKey()=%s]",
                              allowSnapshots, allowReleases, getName(), getKey() );
    }

}
