package org.commonjava.aprox.action;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.AproxLifecycleEvent;

public class AproxLifecycleEventManager
{
    @Inject
    private Event<AproxLifecycleEvent> events;

    public void fireStarted()
    {
        fire( new AproxLifecycleEvent( AproxLifecycleEvent.Type.started ) );
    }

    private void fire( final AproxLifecycleEvent event )
    {
        if ( events != null )
        {
            events.fire( event );
        }
    }

}
