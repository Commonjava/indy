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
package org.commonjava.indy.folo.dto;

import java.util.Set;

public class TrackingIdsDTO
{

    private Set<String> inProgress;

    private Set<String> sealed;

    public TrackingIdsDTO()
    {
    }

    public TrackingIdsDTO( final Set<String> inProgress, final Set<String> sealed )
    {
        this.inProgress = inProgress;
        this.sealed = sealed;
    }

    public Set<String> getInProgress()
    {
        return inProgress;
    }

    public void setInProgress( Set<String> inProgress )
    {
        this.inProgress = inProgress;
    }

    public Set<String> getSealed()
    {
        return sealed;
    }

    public void setSealed( Set<String> sealed )
    {
        this.sealed = sealed;
    }
}
