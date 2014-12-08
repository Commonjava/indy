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
package org.commonjava.aprox.autoprox.fixture;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.autoprox.data.AutoProxRule;
import org.commonjava.aprox.autoprox.data.AutoProxRuleException;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;

public class TestAutoProxFactory
    implements AutoProxRule
{

    private final HttpTestFixture http;

    public TestAutoProxFactory( final HttpTestFixture http )
    {
        this.http = http;
    }

    @Override
    public RemoteRepository createRemoteRepository( final String named )
        throws AutoProxRuleException
    {
        return new RemoteRepository( named, http.formatUrl( "target", named ) );
    }

    @Override
    public HostedRepository createHostedRepository( final String named )
    {
        return new HostedRepository( named );
    }

    @Override
    public Group createGroup( final String named )
    {
        final List<StoreKey> constituents = new ArrayList<StoreKey>();

        constituents.add( new StoreKey( StoreType.hosted, named ) );
        constituents.add( new StoreKey( StoreType.remote, named ) );
        constituents.add( new StoreKey( StoreType.remote, "first" ) );
        constituents.add( new StoreKey( StoreType.remote, "second" ) );

        return new Group( named, constituents );
    }

    @Override
    public String getRemoteValidationPath()
    {
        return null;
    }

    @Override
    public boolean matches( final String name )
    {
        return true;
    }

    @Override
    public RemoteRepository createGroupValidationRemote( final String name )
        throws AutoProxRuleException
    {
        return createRemoteRepository( name );
    }

    @Override
    public boolean isValidationEnabled()
    {
        return true;
    }

    @Override
    public RemoteRepository createValidationRemote( final String name )
        throws AutoProxRuleException, MalformedURLException
    {
        return createRemoteRepository( name );
    }

}
