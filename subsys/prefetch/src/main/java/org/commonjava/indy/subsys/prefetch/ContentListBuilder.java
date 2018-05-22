package org.commonjava.indy.subsys.prefetch;

import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.RemoteRepository;

import java.util.List;

/**
 * Used for generate the root content for remote repository, which provide the initial downloading list to {@link PrefetchManager}
 * to schedule the starting downloading list for the remote.
 */
public interface ContentListBuilder
{
    List<StoreResource> buildContent( final RemoteRepository repository);

    String type();
}
