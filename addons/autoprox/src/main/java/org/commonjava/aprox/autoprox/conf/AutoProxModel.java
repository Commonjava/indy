package org.commonjava.aprox.autoprox.conf;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;
import javax.inject.Singleton;

import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;

@Singleton
@Alternative
@Named( "dont-use-directly" )
public class AutoProxModel
{

    private boolean enabled;

    private boolean deployEnabled;

    private Repository repo;

    private DeployPoint deploy;

    private Group group;

    private String repoValidationPath;

    public final boolean isEnabled()
    {
        return enabled;
    }

    public final Repository getRepo()
    {
        return repo;
    }

    public final DeployPoint getDeploy()
    {
        return deploy;
    }

    public final Group getGroup()
    {
        return group;
    }

    public final void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public final void setRepo( final Repository repo )
    {
        this.repo = repo;
    }

    public final void setDeploy( final DeployPoint deploy )
    {
        this.deploy = deploy;
    }

    public final void setGroup( final Group group )
    {
        this.group = group;
    }

    public final boolean isDeployEnabled()
    {
        return deployEnabled;
    }

    public final void setDeployEnabled( final boolean deployEnabled )
    {
        this.deployEnabled = deployEnabled;
    }

    public String getRepoValidationPath()
    {
        return repoValidationPath;
    }

    public void setRepoValidationPath( final String path )
    {
        this.repoValidationPath = path;
    }

}
