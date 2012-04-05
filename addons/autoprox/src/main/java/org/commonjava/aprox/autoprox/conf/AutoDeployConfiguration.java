package org.commonjava.aprox.autoprox.conf;

public interface AutoDeployConfiguration
{

    boolean isDeployEnabled();

    boolean isSnapshotsEnabled();

    boolean isReleasesEnabled();

    Integer getSnapshotTimeoutSeconds();
}
