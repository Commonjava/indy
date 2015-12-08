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
package org.commonjava.indy.depgraph.model.builder;

import org.commonjava.indy.depgraph.model.DownlogRequest;
import org.commonjava.cartographer.request.ExtraCT;
import org.commonjava.cartographer.request.GraphComposition;
import org.commonjava.cartographer.request.build.RepositoryContentRequestBuilder;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.Location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 8/12/15.
 */
public class DownlogRequestBuilder<T extends DownlogRequestBuilder<T, R>, R extends DownlogRequest>
    extends RepositoryContentRequestBuilder<T, R>
{

    public static final class StandaloneDownlogBuilder
                    extends DownlogRequestBuilder<StandaloneDownlogBuilder, DownlogRequest>
    {
    }

    public static StandaloneDownlogBuilder newDownlogRequestBuilder()
    {
        return new StandaloneDownlogBuilder();
    }

    private boolean pathOnly;

    private String linePrefix;

    public T withPathOnly( boolean pathOnly )
    {
        this.pathOnly = pathOnly;
        return self;
    }

    public T withLinePrefix( String linePrefix )
    {
        this.linePrefix = linePrefix;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final R recipe = (R) new DownlogRequest();
        configure( recipe );

        return recipe;
    }

    protected void confgure( R recipe )
    {
        recipe.setPathOnly( pathOnly );
        recipe.setLinePrefix( linePrefix );
        super.configure( recipe );
    }

    @Override
    public T withExcludedSources( Set<String> excludedSources )
    {
        return super.withExcludedSources( excludedSources );
    }

    @Override
    public T withExtras( Set<ExtraCT> extras )
    {
        return super.withExtras( extras );
    }

    @Override
    public T withMetas( Set<String> metas )
    {
        return super.withMetas( metas );
    }

    @Override
    public T withExcludedSourceLocations( Set<Location> excludedSourceLocations )
    {
        return super.withExcludedSourceLocations( excludedSourceLocations );
    }

    @Override
    public T withMultiSourceGAVs( boolean multiSourceGAVs )
    {
        return super.withMultiSourceGAVs( multiSourceGAVs );
    }

    @Override
    public T withLocalUrls( boolean localUrls )
    {
        return super.withLocalUrls( localUrls );
    }

    @Override
    public T withGraphs( GraphComposition graphs )
    {
        return super.withGraphs( graphs );
    }

    @Override
    public T withSource( String source )
    {
        return super.withSource( source );
    }

    @Override
    public T withWorkspaceId( String workspaceId )
    {
        return super.withWorkspaceId( workspaceId );
    }

    @Override
    public T withSourceLocation( Location source )
    {
        return super.withSourceLocation( source );
    }

    @Override
    public T withTimeoutSecs( Integer timeoutSecs )
    {
        return super.withTimeoutSecs( timeoutSecs );
    }

    @Override
    public T withPatcherIds( Collection<String> patcherIds )
    {
        return super.withPatcherIds( patcherIds );
    }

    @Override
    public T withResolve( boolean resolve )
    {
        return super.withResolve( resolve );
    }

    @Override
    public T withInjectedBOMs( List<ProjectVersionRef> injectedBOMs )
    {
        return super.withInjectedBOMs( injectedBOMs );
    }

    @Override
    public T withExcludedSubgraphs( Collection<ProjectVersionRef> excludedSubgraphs )
    {
        return super.withExcludedSubgraphs( excludedSubgraphs );
    }

    @Override
    public T withVersionSelections( Map<ProjectRef, ProjectVersionRef> versionSelections )
    {
        return super.withVersionSelections( versionSelections );
    }
}
