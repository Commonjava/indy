package org.commonjava.indy.change;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;

/**
 * Common way to insulate the system from event processing failures. Event handling is derivative or peripheral
 * to the main user workflows, even though it can harm long-term operation of the system. However, we can still
 * serve content if even handling is malfunctioning...and errors in event handling cannot be fixed by the user
 * anyway.
 *
 * Created by jdcasey on 11/1/17.
 */
public class EventUtils
{
    public static <T> void fireEvent( Event<T> dispatcher, T event )
    {
        Logger logger = LoggerFactory.getLogger( EventUtils.class );
        try
        {
            if ( dispatcher != null )
            {
                logger.trace( "Firing event: {}", event );
                dispatcher.fire( event );
            }
            else
            {
                logger.error( "Cannot fire event: {}. Reason: Event dispatcher is null!", event );
            }
        }
        catch ( RuntimeException e )
        {
            logger.error( String.format( "Error processing event: %s. Reason: %s", event, e.getMessage() ), e );
        }
    }
}
