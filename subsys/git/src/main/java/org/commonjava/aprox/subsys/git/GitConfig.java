package org.commonjava.aprox.subsys.git;

import java.io.File;

public class GitConfig
{

    private final File dir;

    private final String url;

    public GitConfig( final File dir, final String url )
    {
        this.dir = dir;
        this.url = url;
    }

    public File getContentDir()
    {
        return dir;
    }

    public String getCloneFrom()
    {
        return url;
    }

}
