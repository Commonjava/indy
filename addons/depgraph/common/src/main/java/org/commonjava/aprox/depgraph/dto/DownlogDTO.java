package org.commonjava.aprox.depgraph.dto;

import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;

public class DownlogDTO
    extends RepositoryContentRecipe
{

    private boolean pathOnly;

    private String linePrefix;

    public boolean isPathOnly()
    {
        return pathOnly;
    }

    public void setPathOnly( final boolean pathOnly )
    {
        this.pathOnly = pathOnly;
    }

    public String getLinePrefix()
    {
        return linePrefix;
    }

    public void setLinePrefix( final String linePrefix )
    {
        this.linePrefix = linePrefix;
    }

}
