package org.commonjava.web.maven.proxy.change.event;

import java.util.Collection;

import org.commonjava.couch.change.j2ee.AbstractUpdateEvent;
import org.commonjava.web.maven.proxy.model.Repository;

public class RepositoryUpdateEvent
    extends AbstractUpdateEvent<Repository>
{

    private final ProxyManagerUpdateType type;

    public RepositoryUpdateEvent( final ProxyManagerUpdateType type,
                                  final Collection<Repository> changes )
    {
        super( changes );
        this.type = type;
    }

    public RepositoryUpdateEvent( final ProxyManagerUpdateType type, final Repository... changes )
    {
        super( changes );
        this.type = type;
    }

    public ProxyManagerUpdateType getType()
    {
        return type;
    }

}
