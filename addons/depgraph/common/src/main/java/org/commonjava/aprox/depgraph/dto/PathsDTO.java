package org.commonjava.aprox.depgraph.dto;

import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;

public class PathsDTO 
    extends WebOperationConfigDTO
{

    /** The target artifacts which we want collect paths to. */
    private Set<ProjectRef> targets;

    /**
     * @return the target artifacts which we want collect paths to
     */
    public Set<ProjectRef> getTargets()
    {
        return targets;
    }

    /**
     * @param targets
     *            the target artifacts which we want collect paths to
     */
    public void setTargets( final Set<ProjectRef> targets )
    {
        this.targets = targets;
    }

}
