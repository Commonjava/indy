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

import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public class TestAutoProxFactory
    implements AutoProxFactory
{

    private final HttpTestFixture http;

    public TestAutoProxFactory( final HttpTestFixture http )
    {
        this.http = http;
    }

    @Override
    public RemoteRepository createRemoteRepository( final String named )
        throws MalformedURLException
    {
        return new RemoteRepository( named, http.formatUrl( "target", named ) );
    }

    @Override
    public HostedRepository createHostedRepository( final String named )
    {
        return new HostedRepository( named );
    }

    @Override
    public Group createGroup( final String named, final RemoteRepository remote, final HostedRepository hosted )
    {
        List<StoreKey> constituents = new ArrayList<StoreKey>();
        if ( hosted != null )
        {
            constituents.add( hosted.getKey() );
        }
        if ( remote != null )
        {
            constituents.add( remote.getKey() );
        }
        
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

}
