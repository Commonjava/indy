package org.commonjava.aprox.subsys.git;

import java.io.File;

public class GitConfig
{

    private final File dir;

    private final String url;

    private final boolean commitFileManifestsEnabled;

    private String remoteBranchName;

    private String userEmail;

    public GitConfig( final File dir, final String url, final boolean commitFileManifestsEnabled )
    {
        this.dir = dir;
        this.url = url;
        this.commitFileManifestsEnabled = commitFileManifestsEnabled;
    }

    public File getContentDir()
    {
        return dir;
    }

    public String getCloneFrom()
    {
        return url;
    }

    public boolean isCommitFileManifestsEnabled()
    {
        return commitFileManifestsEnabled;
    }

    public GitConfig setRemoteBranchName( final String remoteBranchName )
    {
        if ( remoteBranchName != null )
        {
            this.remoteBranchName = remoteBranchName;
        }

        return this;
    }

    public String getRemoteBranchName()
    {
        return remoteBranchName == null ? "master" : remoteBranchName;
    }

    public GitConfig setUserEmail( final String userEmail )
    {
        if ( userEmail != null )
        {
            this.userEmail = userEmail;
        }

        return this;
    }

    public String getUserEmail()
    {
        return userEmail;
    }
}
