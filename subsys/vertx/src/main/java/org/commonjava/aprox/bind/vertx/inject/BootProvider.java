package org.commonjava.aprox.bind.vertx.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.impl.DefaultVertx;

@ApplicationScoped
public class BootProvider
{

    private final DefaultVertx vertx = new DefaultVertx();

    @Produces
    public Vertx getVertx()
    {
        return vertx;
    }

    @Produces
    public EventBus getEventBus()
    {
        return vertx.eventBus();
    }

}
