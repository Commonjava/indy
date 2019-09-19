/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.datafile.change;

import org.commonjava.indy.audit.ChangeSummary;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.File;

import static org.commonjava.indy.change.EventUtils.fireEvent;

/**
 * Helper class to provide simple methods to handle null-checking, etc. around the firing of Indy filesystem events.
 */
@ApplicationScoped
public class DataFileEventManager
{

    @Inject
    private Event<DataFileEvent> events;

    public void fire( final DataFileEvent evt )
    {
        fireEvent( events, evt );
    }

    public void accessed( final File file )
    {
        fire( new DataFileEvent( file ) );
    }

    public void modified( final File file, final ChangeSummary summary )
    {
        fire( new DataFileEvent( file, DataFileEventType.modified, summary ) );
    }

    public void deleted( final File file, final ChangeSummary summary )
    {
        fire( new DataFileEvent( file, DataFileEventType.deleted, summary ) );
    }
}
