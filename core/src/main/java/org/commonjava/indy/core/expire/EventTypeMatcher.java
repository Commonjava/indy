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
package org.commonjava.indy.core.expire;

import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

public class EventTypeMatcher
    extends GroupMatcher<TriggerKey>
{

    private static final long serialVersionUID = 1L;

    public EventTypeMatcher( final String eventType )
    {
        super( ScheduleManager.groupNameSuffix( eventType ), StringOperatorName.ENDS_WITH );
    }

}
