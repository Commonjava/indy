/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.subsys.http.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.commonjava.aprox.model.Repository;
import org.commonjava.util.logging.Logger;

public class TLRepositoryCredentialsProvider
    implements CredentialsProvider
{

    private final Logger logger = new Logger( getClass() );

    private final ThreadLocal<Map<AuthScope, Credentials>> credentials = new ThreadLocal<Map<AuthScope, Credentials>>();

    private final ThreadLocal<Map<AuthScope, Repository>> repositories = new ThreadLocal<Map<AuthScope, Repository>>();

    public synchronized void bind( final Collection<Repository> repositories )
    {
        if ( repositories != null )
        {
            final Map<AuthScope, Credentials> creds = new HashMap<AuthScope, Credentials>();
            final Map<AuthScope, Repository> repos = new HashMap<AuthScope, Repository>();
            for ( final Repository repository : repositories )
            {
                final AuthScope as = new AuthScope( repository.getHost(), repository.getPort() );
                logger.info( "Storing repository def: %s under authscope: %s:%d", repository.getName(),
                             repository.getHost(), repository.getPort() );

                //FIXME: Seems like multiple repos with same host/port could easily cause confusion if they're not configured the same way later on...
                repos.put( as, repository );

                if ( repository.getUser() != null )
                {
                    creds.put( as, new UsernamePasswordCredentials( repository.getUser(), repository.getPassword() ) );
                }

                if ( repository.getProxyHost() != null && repository.getProxyUser() != null )
                {
                    creds.put( new AuthScope( repository.getProxyHost(), repository.getProxyPort() ),
                               new UsernamePasswordCredentials( repository.getProxyUser(),
                                                                repository.getProxyPassword() ) );
                }
            }

            this.credentials.set( creds );
            this.repositories.set( repos );
        }
    }

    public Repository getRepository( final String host, final int port )
    {
        logger.info( "Looking up repository def under authscope: %s:%d", host, port );

        final Map<AuthScope, Repository> repos = repositories.get();
        if ( repos == null )
        {
            return null;
        }

        //FIXME: Seems like multiple repos with same host/port could easily cause confusion if they're not configured the same way later on...
        return repos.get( new AuthScope( host, port ) );
    }

    public void bind( final Repository... repositories )
    {
        bind( Arrays.asList( repositories ) );
    }

    @Override
    public void clear()
    {
        credentials.set( null );
    }

    @Override
    public synchronized void setCredentials( final AuthScope authscope, final Credentials creds )
    {
        Map<AuthScope, Credentials> map = credentials.get();
        if ( map == null )
        {
            map = new HashMap<AuthScope, Credentials>();
            credentials.set( map );
        }
        map.put( authscope, creds );
    }

    @Override
    public Credentials getCredentials( final AuthScope authscope )
    {
        final Map<AuthScope, Credentials> map = credentials.get();
        return map.get( authscope );
    }

}
