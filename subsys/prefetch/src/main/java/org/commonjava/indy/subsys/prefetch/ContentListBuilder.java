/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.prefetch;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.prefetch.models.RescanablePath;
import org.commonjava.maven.galley.model.ConcreteResource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Used for generate the root content for remote repository, which provide the initial downloading list to {@link PrefetchManager}
 * to schedule the starting downloading list for the remote.
 */
public interface ContentListBuilder
{
    List<ConcreteResource> buildContent( final RemoteRepository repository, boolean isRescan);

    default List<ConcreteResource> buildContent( final RemoteRepository repository )
    {
        return buildContent( repository, false );
    }

    default List<String> buildPaths( RemoteRepository repository )
    {
        return buildContent( repository ).stream().map( r -> r.getPath() ).collect( Collectors.toList() );
    }

    default List<RescanablePath> buildPaths( RemoteRepository repository, boolean isRescan )
    {
        return buildContent( repository, isRescan ).stream().map( r -> new RescanablePath( r.getPath(), isRescan ) ).collect( Collectors.toList() );
    }

    String type();
}
