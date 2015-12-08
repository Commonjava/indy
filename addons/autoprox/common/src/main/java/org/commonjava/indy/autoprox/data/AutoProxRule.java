/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

public interface AutoProxRule
{

    String DEFAULT_FACTORY_SCRIPT = "default.groovy";

    boolean isValidationEnabled();

    boolean matches( String name );

    RemoteRepository createRemoteRepository( String named )
        throws AutoProxRuleException, MalformedURLException;

    HostedRepository createHostedRepository( String named );

    Group createGroup( String named );

    /**
     * MAY be null IF the group doesn't require validation vs. the remote URL of its associated auto-created remote repository.
     * Otherwise, this repository should supply any credentials/configuration needed to validate the remote URL.
     * 
     * <br/>
     * NOTE: Use {@link AutoProxRule#createValidationRemote(String)} instead.
     */
    @Deprecated
    RemoteRepository createGroupValidationRemote( String name )
        throws AutoProxRuleException, MalformedURLException;

    String getRemoteValidationPath();

    /**
     * MAY be null IF the remotes/groups don't require validation.
     * Otherwise, this repository should supply any credentials/configuration needed to validate the remote URL.
     */
    RemoteRepository createValidationRemote( String name )
        throws AutoProxRuleException, MalformedURLException;

}
