package org.commonjava.aprox.change.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

@ApplicationScoped
public class FileEventManager
{

    @Inject
    private Event<FileStorageEvent> storageEvent;

    @Inject
    private Event<FileAccessEvent> accessEvent;

    @Inject
    private Event<FileDeletionEvent> deleteEvent;

    @Inject
    private Event<FileErrorEvent> errorEvent;

    @Inject
    private Event<FileNotFoundEvent> notFoundEvent;

    public void fire( final FileNotFoundEvent evt )
    {
        doFire( notFoundEvent, evt );
    }

    public void fire( final FileStorageEvent evt )
    {
        doFire( storageEvent, evt );
    }

    public void fire( final FileAccessEvent evt )
    {
        doFire( accessEvent, evt );
    }

    public void fire( final FileDeletionEvent evt )
    {
        doFire( deleteEvent, evt );
    }

    public void fire( final FileErrorEvent evt )
    {
        doFire( errorEvent, evt );
    }

    private <T> void doFire( final Event<T> eventQ, final T evt )
    {
        if ( eventQ != null )
        {
            eventQ.fire( evt );
        }
    }
}
