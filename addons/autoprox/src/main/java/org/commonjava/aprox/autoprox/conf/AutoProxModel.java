package org.commonjava.aprox.autoprox.conf;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;

@javax.enterprise.context.ApplicationScoped
@Alternative
@Named( "dont-use-directly" )
public class AutoProxModel
{

    private Repository repo;

    private DeployPoint deploy;

    private Group group;

    private String repoValidationPath;

    public Repository getRepo()
    {
        return repo;
    }

    public DeployPoint getDeploy()
    {
        return deploy;
    }

    public Group getGroup()
    {
        return group;
    }

    public void setRepo( final Repository repo )
    {
        this.repo = repo;
    }

    public void setDeploy( final DeployPoint deploy )
    {
        this.deploy = deploy;
    }

    public void setGroup( final Group group )
    {
        this.group = group;
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
