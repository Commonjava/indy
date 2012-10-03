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

public class TLRepositoryCredentialsProvider
    implements CredentialsProvider
{

    private final ThreadLocal<Map<AuthScope, Credentials>> credentials = new ThreadLocal<Map<AuthScope, Credentials>>();

    public synchronized void bind( final Collection<Repository> repositories )
    {
        if ( repositories != null )
        {
            final Map<AuthScope, Credentials> repos = new HashMap<AuthScope, Credentials>();
            for ( final Repository repository : repositories )
            {
                if ( repository.getUser() != null )
                {
                    repos.put( new AuthScope( repository.getHost(), repository.getPort() ),
                               new UsernamePasswordCredentials( repository.getUser(), repository.getPassword() ) );
                }

                if ( repository.getProxyHost() != null && repository.getProxyUser() != null )
                {
                    repos.put( new AuthScope( repository.getProxyHost(), repository.getProxyPort() ),
                               new UsernamePasswordCredentials( repository.getProxyUser(),
                                                                repository.getProxyPassword() ) );
                }
            }

            credentials.set( repos );
        }
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
