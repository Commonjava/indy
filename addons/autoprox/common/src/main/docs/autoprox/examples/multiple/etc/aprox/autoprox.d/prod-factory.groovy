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

import org.commonjava.aprox.autoprox.conf.AutoProxFactory;

import java.net.MalformedURLException;

import org.commonjava.aprox.model.*;

class ProdFactory implements AutoProxFactory
{
    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        def match = (named =~ /prod-([^0-9]+)([0-9])(.+)/)[0]
        new RemoteRepository( name: named, url: "http://repository.myco.com/products/${match[1] + match[2]}/${match[2] + match[3]}/" )
    }

    HostedRepository createHostedRepository( String named )
    {
        new HostedRepository( named )
    }

    Group createGroup( String named, RemoteRepository remote, HostedRepository hosted )
    {
        Group g = new Group( named );
        g.addConstituent( new StoreKey( StoreType.remote, "central" ) )
        g.addConstituent( new StoreKey( StoreType.remote, named ) )
        g.addConstituent( new StoreKey( StoreType.hosted, named ) )
        
        g
    }

    String getRemoteValidationPath()
    {
        null
    }
}