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
package org.commonjava.indy.subsys.git;

import java.util.Collection;
import java.util.Date;

/**
 * Created by ruhan on 5/7/18.
 */
public class ChangelogEntry
{
    private String username;

    private String message;

    private Collection<String> paths; // file(s) affected

    private Date timestamp;

    public ChangelogEntry( String user, String message, Collection<String> paths )
    {
        this.username = user;
        this.message = message;
        this.paths = paths;
        this.timestamp = new Date();
    }

    public String getUsername()
    {
        return username;
    }

    public String getMessage()
    {
        return message;
    }

    public Collection<String> getPaths()
    {
        return paths;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }
}
