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
package org.commonjava.indy.promote.change.event;

import org.commonjava.indy.promote.model.PathsPromoteResult;

/**
 * Created by ruhan on 3/13/18.
 */
public class PathsPromoteCompleteEvent extends PromoteCompleteEvent
{

    private PathsPromoteResult promoteResult;

    public PathsPromoteCompleteEvent( PathsPromoteResult result )
    {
        this.promoteResult = result;
    }

    public PathsPromoteResult getPromoteResult()
    {
        return promoteResult;
    }

    @Override
    public String toString()
    {
        return "PathsPromoteCompleteEvent{" + "promoteResult=" + promoteResult + '}';
    }
}
