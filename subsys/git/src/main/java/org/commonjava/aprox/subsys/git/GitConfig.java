package org.commonjava.aprox.subsys.git;

import java.io.File;

public class GitConfig
{

    private final File dir;

    private final String url;

    private final boolean commitFileManifestsEnabled;

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

}
