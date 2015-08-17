package org.commonjava.aprox.depgraph.model;

import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class UrlMapProject
{
    private String repoUrl;

    private Set<String> files;

    public UrlMapProject(){}

    public UrlMapProject( final String url, final Set<String> files )
    {
        repoUrl = url;
        this.files = files;
    }

    public String getRepoUrl()
    {
        return repoUrl;
    }

    public void setRepoUrl( final String repoUrl )
    {
        this.repoUrl = repoUrl;
    }

    public Set<String> getFiles()
    {
        return files;
    }

    public void setFiles( final Set<String> files )
    {
        this.files = files;
    }
}
