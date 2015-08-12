package org.commonjava.aprox.depgraph.model;

import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class WorkspaceList
{

    private Set<String> workspaces;

    public WorkspaceList(){}

    public WorkspaceList( Set<String> workspaces )
    {
        this.workspaces = workspaces;
    }

    public Set<String> getWorkspaces()
    {
        return workspaces;
    }

    public void setWorkspaces( Set<String> workspaces )
    {
        this.workspaces = workspaces;
    }
}
