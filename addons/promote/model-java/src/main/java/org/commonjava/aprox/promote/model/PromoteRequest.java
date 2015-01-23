package org.commonjava.aprox.promote.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.model.core.StoreKey;

/**
 * Configuration for promoting artifacts from one store to another (denoted by their corresponding {@link StoreKey}'s). If paths are provided, only
 * promote a subset of the content in the source store, otherwise promote all.
 * 
 * @author jdcasey
 *
 */
public class PromoteRequest
{

    private StoreKey source;

    private StoreKey target;

    private Set<String> paths;

    private boolean purgeSource;

    private boolean dryRun;

    public PromoteRequest()
    {
    }

    public PromoteRequest( final StoreKey source, final StoreKey target, final Set<String> paths )
    {
        this.source = source;
        this.target = target;
        this.paths = paths;
    }

    public PromoteRequest( final StoreKey source, final StoreKey target, final String... paths )
    {
        this.source = source;
        this.target = target;
        this.paths = new HashSet<>( Arrays.asList( paths ) );
    }

    public StoreKey getSource()
    {
        return source;
    }

    public PromoteRequest setSource( final StoreKey source )
    {
        this.source = source;
        return this;
    }

    public StoreKey getTarget()
    {
        return target;
    }

    public PromoteRequest setTarget( final StoreKey target )
    {
        this.target = target;
        return this;
    }

    public Set<String> getPaths()
    {
        return paths == null ? Collections.<String> emptySet() : paths;
    }

    public PromoteRequest setPaths( final Set<String> paths )
    {
        this.paths = paths;
        return this;
    }

    @Override
    public String toString()
    {
        return String.format( "PromoteRequest [source=%s, target=%s, paths=%s]", source, target, paths );
    }

    public PromoteRequest setPurgeSource( final boolean purgeSource )
    {
        this.purgeSource = purgeSource;
        return this;
    }

    public boolean isPurgeSource()
    {
        return purgeSource;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public PromoteRequest setDryRun( final boolean dryRun )
    {
        this.dryRun = dryRun;
        return this;
    }

}
