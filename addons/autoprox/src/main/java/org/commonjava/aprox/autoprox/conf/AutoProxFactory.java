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
package org.commonjava.aprox.autoprox.conf;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;

public interface AutoProxFactory
{

    String LEGACY_FACTORY_NAME = "legacy-factory.groovy";

    String DEFAULT_FACTORY_SCRIPT = "default.groovy";

    boolean matches( String name );

    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException;

    HostedRepository createHostedRepository( String named );

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted );

    String getRemoteValidationPath();

}
