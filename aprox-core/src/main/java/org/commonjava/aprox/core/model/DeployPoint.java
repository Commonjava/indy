package org.commonjava.aprox.core.model;

import com.google.gson.annotations.SerializedName;

public class DeployPoint
    extends AbstractArtifactStore
{

    @SerializedName( "allow_snapshots" )
    private boolean allowSnapshots = false;

    @SerializedName( "allow_releases" )
    private boolean allowReleases = true;

    DeployPoint()
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

}
