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
package org.commonjava.indy.audit;

import java.util.Date;

/**
 * Describes the reason for a particular change to the system, usually in terms of store definitions, templates, add-on configurations, almost 
 * anything stored in the Indy data directory (persistent system-wide state), along with the user that generated the change.
 */
public class ChangeSummary
{
    public static final String SYSTEM_USER = "system";

    private final String user;

    private final Date timestamp;

    private final String summary;

    private final String revisionId;

    public ChangeSummary( final String user, final String summary )
    {
        this.user = user;
        this.summary = summary;
        this.timestamp = new Date();
        this.revisionId = null;
    }

    public ChangeSummary( final String user, final String summary, final Date timestamp, final String revisionId )
    {
        this.user = user;
        this.summary = summary;
        this.timestamp = timestamp;
        this.revisionId = revisionId;
    }

    public String getUser()
    {
        return user;
    }

    public String getSummary()
    {
        return summary;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    @Override
    public String toString()
    {
        return String.format( "[%s; %s] %s", user, timestamp, summary );
    }

    public String getRevisionId()
    {
        return revisionId;
    }

}
