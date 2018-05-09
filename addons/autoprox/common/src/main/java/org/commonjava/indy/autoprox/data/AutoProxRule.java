/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.data;

import java.net.MalformedURLException;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;

public interface AutoProxRule
{

    String DEFAULT_FACTORY_SCRIPT = "default.groovy";

    boolean isValidationEnabled();

    boolean matches( StoreKey key );

    RemoteRepository createRemoteRepository( StoreKey key )
        throws AutoProxRuleException, MalformedURLException;

    HostedRepository createHostedRepository( StoreKey key );

    Group createGroup( StoreKey key );

    String getRemoteValidationPath();

    /**
     * MAY be null IF the remotes/groups don't require validation.
     * Otherwise, this repository should supply any credentials/configuration needed to validate the remote URL.
     */
    RemoteRepository createValidationRemote( StoreKey key )
        throws AutoProxRuleException, MalformedURLException;

}
