package org.commonjava.aprox.promote.model;

import java.util.Collections;
import java.util.Set;

/**
 * Contains the result of a promotion attempt. If the promotion is a success, the pending paths and error will be <b>null</b>. Otherwise, these are
 * populated to support the resume feature (for transient or correctable errors).
 * 
 * @author jdcasey
 *
 */
public class PromoteResult
{

    private PromoteRequest request;

    private Set<String> pendingPaths;

    private Set<String> completedPaths;

    private String error;

    public PromoteResult()
    {
    }

    public PromoteResult( final PromoteRequest request, final Set<String> pending,
                          final Set<String> complete,
                          final String error )
    {
        this.request = request;
        this.pendingPaths = pending;
        this.completedPaths = complete;
        this.error = error;
    }

    public Set<String> getPendingPaths()
    {
        return pendingPaths == null ? Collections.<String> emptySet() : pendingPaths;
    }

    public void setPendingPaths( final Set<String> pendingPaths )
    {
        this.pendingPaths = pendingPaths;
    }

    public Set<String> getCompletedPaths()
    {
        return completedPaths == null ? Collections.<String> emptySet() : completedPaths;
    }

    public void setCompletedPaths( final Set<String> completedPaths )
    {
        this.completedPaths = completedPaths;
    }

    public String getError()
    {
        return error;
    }

    public void setError( final String error )
    {
        this.error = error;
    }

    public PromoteRequest getRequest()
    {
        return request;
    }

    public void setRequest( final PromoteRequest request )
    {
        this.request = request;
    }

    @Override
    public String toString()
    {
        return String.format( "PromoteResult [\n  request=%s\n  pendingPaths=%s\n  completedPaths=%s\n  error=%s\n]",
                              request, pendingPaths, completedPaths, error );
    }

}
