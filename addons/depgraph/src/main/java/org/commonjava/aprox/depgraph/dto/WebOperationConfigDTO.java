package org.commonjava.aprox.depgraph.dto;

import java.net.URISyntaxException;
import java.util.Set;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryConfig;

public class WebOperationConfigDTO
{

    private Set<ProjectVersionRef> roots;

    private Set<ExtraCT> extras;

    private String workspaceId;

    private String preset;

    private StoreKey source;

    private boolean resolve;

    private Integer timeoutSecs;

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public String getWorkspaceId()
    {
        return workspaceId;
    }

    public String getPreset()
    {
        return preset;
    }

    public StoreKey getSource()
    {
        return source;
    }

    public void setRoots( final Set<ProjectVersionRef> roots )
    {
        this.roots = roots;
    }

    public void setWorkspaceId( final String workspaceId )
    {
        this.workspaceId = workspaceId;
    }

    public void setPreset( final String preset )
    {
        this.preset = preset;
    }

    public void setSource( final StoreKey source )
    {
        this.source = source;
    }

    @Override
    public String toString()
    {
        return String.format( "WebOperationConfigDTO [roots=%s, workspaceId=%s, preset=%s, source=%s]", roots,
                              workspaceId, preset, source );
    }

    public boolean isValid()
    {
        return source != null && roots != null && !roots.isEmpty();
    }

    public DiscoveryConfig getDiscoveryConfig()
        throws URISyntaxException
    {
        final DefaultDiscoveryConfig ddc = new DefaultDiscoveryConfig( source.toString() );
        ddc.setEnabled( true );

        return ddc;
    }

    public Set<ExtraCT> getExtras()
    {
        return extras;
    }

    public void setExtras( final Set<ExtraCT> extras )
    {
        this.extras = extras;
    }

    public boolean isResolve()
    {
        return resolve;
    }

    public void setResolve( final boolean resolve )
    {
        this.resolve = resolve;
    }

    public int getTimeoutSecs()
    {
        return timeoutSecs == null ? 10 : timeoutSecs;
    }

    public void setTimeoutSecs( final int timeoutSecs )
    {
        this.timeoutSecs = timeoutSecs;
    }

}
