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
package org.commonjava.indy.httprox.handler;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ruhan on 4/12/18.
 */
public class ProxyCreationResult
{
    private Group group;

    private HostedRepository hosted;

    private RemoteRepository remote;

    // TODO: 4/16/18, is this really useful?
    public List<StoreKey> getStores()
    {
        return Arrays.asList( hosted.getKey(), remote.getKey() ); // contains (hosted, remote) in that order
    }

    public Group getGroup()
    {
        return group;
    }

    public void setGroup( Group group )
    {
        this.group = group;
    }

    public HostedRepository getHosted()
    {
        return hosted;
    }

    public void setHosted( HostedRepository hosted )
    {
        this.hosted = hosted;
    }

    public RemoteRepository getRemote()
    {
        return remote;
    }

    public void setRemote( RemoteRepository remote )
    {
        this.remote = remote;
    }
}
