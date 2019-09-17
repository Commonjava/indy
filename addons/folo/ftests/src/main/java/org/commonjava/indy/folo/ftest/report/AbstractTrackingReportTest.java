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
package org.commonjava.indy.folo.ftest.report;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Before;

public abstract class AbstractTrackingReportTest
    extends AbstractIndyFunctionalTest
{

    protected static final String STORE = "test";

    protected static final String CENTRAL = "central";

    protected static final String PUBLIC = "public";

    @Before
    public void before()
        throws Exception
    {
        if ( !createStandardStores() )
        {
            return;
        }

        final String changelog = "Setup " + name.getMethodName();
        final HostedRepository hosted =
            this.client.stores()
                       .create( new HostedRepository( STORE ), changelog, HostedRepository.class );

        RemoteRepository central = null;
        if ( !client.stores()
                    .exists( remote, CENTRAL ) )
        {
            central =
                client.stores()
                      .create( new RemoteRepository( CENTRAL, "http://repo.maven.apache.org/maven2/" ), changelog,
                               RemoteRepository.class );
        }
        else
        {
            central = client.stores()
                            .load( remote, CENTRAL, RemoteRepository.class );
        }

        Group g;
        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            g = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            g = client.stores()
                      .create( new Group( PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey(), central.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

    protected boolean createStandardStores()
    {
        return true;
    }

    protected String sha256Hex( final byte[] bytes )
        throws Exception
    {
        System.out.println( "sha256" );
        return digest( bytes, MessageDigest.getInstance( "SHA-256" ) );
    }

    protected String md5Hex( final byte[] bytes )
        throws Exception
    {
        System.out.println( "md5" );
        return digest( bytes, MessageDigest.getInstance( "MD5" ) );
    }

    protected String digest( final byte[] bytes, final MessageDigest md )
        throws Exception
    {
        final StringBuilder sb = new StringBuilder();
        for ( final byte b : md.digest( bytes ) )
        {
            final String hex = Integer.toHexString( b & 0xff );
            if ( hex.length() < 2 )
            {
                sb.append( '0' );
            }
            sb.append( hex );
        }

        return sb.toString();
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.asList( new IndyFoloAdminClientModule(), new IndyFoloContentClientModule() );
    }

}
