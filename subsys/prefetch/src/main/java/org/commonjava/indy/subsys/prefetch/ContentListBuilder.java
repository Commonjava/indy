package org.commonjava.indy.subsys.prefetch;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.model.ConcreteResource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Used for generate the root content for remote repository, which provide the initial downloading list to {@link PrefetchManager}
 * to schedule the starting downloading list for the remote.
 */
public interface ContentListBuilder
{
    List<ConcreteResource> buildContent( final RemoteRepository repository);

    default List<String> buildPaths( RemoteRepository repository )
    {
        return buildContent( repository ).stream().map( r -> r.getPath() ).collect( Collectors.toList() );
    }

    String type();
}
