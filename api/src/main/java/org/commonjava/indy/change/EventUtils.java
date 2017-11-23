/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
        try
        {
            if ( dispatcher != null )
            {
                dispatcher.fire( event );
            }
            else
            {
                Logger logger = LoggerFactory.getLogger( EventUtils.class );
                logger.error( "Cannot fire event: {}. Reason: Event dispatcher is null!", event );
            }
        }
        catch ( RuntimeException e )
        {
            Logger logger = LoggerFactory.getLogger( EventUtils.class );
            logger.error( String.format( "Error processing event: %s. Reason: %s", event, e.getMessage() ), e );
        }
    }
}
