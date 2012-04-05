package org.commonjava.aprox.autoprox.conf;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "deploy" )
@Alternative
@Named( "dont-use-directly" )
public class DefaultAutoDeployConfiguration
    implements AutoDeployConfiguration
{

    private boolean deployEnabled = false;

    private boolean releasesEnabled = true;

    private boolean snapshotsEnabled = true;

    private Integer snapshotTimeoutSeconds;

    public DefaultAutoDeployConfiguration( final boolean enabled )
    {
        this.deployEnabled = enabled;
    }

    public DefaultAutoDeployConfiguration()
    {
    }

    @Override
    public Integer getSnapshotTimeoutSeconds()
    {
        return snapshotTimeoutSeconds;
    }

    @ConfigName( "snapshot.timeout.seconds" )
    public void setSnapshotTimeoutSeconds( final int snapshotTimeoutSeconds )
    {
        this.snapshotTimeoutSeconds = snapshotTimeoutSeconds;
    }

    @Override
    public boolean isSnapshotsEnabled()
    {
        return snapshotsEnabled;
    }

    @Override
    public boolean isReleasesEnabled()
    {
        return releasesEnabled;
    }

    @Override
    public boolean isDeployEnabled()
    {
        return deployEnabled;
    }

    @ConfigName( "enabled" )
    public void setDeployEnabled( final boolean deployEnabled )
    {
        this.deployEnabled = deployEnabled;
    }

    @ConfigName( "releases" )
    public void setReleasesEnabled( final boolean releasesEnabled )
    {
        this.releasesEnabled = releasesEnabled;
    }

    @ConfigName( "snapshots" )
    public void setSnapshotsEnabled( final boolean snapshotsEnabled )
    {
        this.snapshotsEnabled = snapshotsEnabled;
    }

}
