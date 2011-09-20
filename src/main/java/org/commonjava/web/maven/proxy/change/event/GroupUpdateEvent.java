package org.commonjava.web.maven.proxy.change.event;

import java.util.Collection;

import org.commonjava.couch.change.j2ee.AbstractUpdateEvent;
import org.commonjava.web.maven.proxy.model.Group;

public class GroupUpdateEvent
    extends AbstractUpdateEvent<Group>
{

    private final ProxyManagerUpdateType type;

    public GroupUpdateEvent( final ProxyManagerUpdateType type, final Collection<Group> changes )
    {
        super( changes );
        this.type = type;
    }

    public GroupUpdateEvent( final ProxyManagerUpdateType type, final Group... changes )
    {
        super( changes );
        this.type = type;
    }

    public ProxyManagerUpdateType getType()
    {
        return type;
    }

}
