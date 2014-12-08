/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.autoprox.data;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;

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
