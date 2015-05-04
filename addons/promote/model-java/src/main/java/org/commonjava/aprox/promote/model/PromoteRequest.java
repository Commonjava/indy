/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
