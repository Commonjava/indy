package org.commonjava.web.maven.proxy.change.event;

import java.util.Collection;

import org.commonjava.couch.change.j2ee.AbstractUpdateEvent;

public class ProxyManagerDeleteEvent
    extends AbstractUpdateEvent<String>
{

    public enum Type
    {
        REPOSITORY, GROUP;
    }

    private final Type type;

    public ProxyManagerDeleteEvent( final Type type, final Collection<String> names )
    {
        super( names );
        this.type = type;
    }

    public ProxyManagerDeleteEvent( final Type type, final String... names )
    {
        super( names );
        this.type = type;
    }

    public Type getType()
    {
        return type;
    }

}
