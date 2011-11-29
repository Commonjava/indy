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
package org.commonjava.aprox.core.rest.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.commonjava.aprox.core.model.Repository;

public class TLRepositoryCredentialsProvider
    implements CredentialsProvider
{

    private final ThreadLocal<Map<String, Repository>> repoBindings =
        new ThreadLocal<Map<String, Repository>>();

    public void bind( final Collection<Repository> repositories )
    {
        if ( repositories != null )
        {
            Map<String, Repository> repos = new HashMap<String, Repository>();
            for ( Repository repository : repositories )
            {
                repos.put( repository.getHost() + ":" + repository.getPort(), repository );
            }

            repoBindings.set( repos );
        }
    }

    public void bind( final Repository... repositories )
    {
        bind( Arrays.asList( repositories ) );
    }

    @Override
    public void clear()
    {
        repoBindings.set( null );
    }

    @Override
    public void setCredentials( final AuthScope authscope, final Credentials credentials )
    {}

    @Override
    public Credentials getCredentials( final AuthScope authscope )
    {
        String key = authscope.getHost() + ":" + authscope.getPort();
        Repository repo = repoBindings.get().get( key );
        if ( repo != null && repo.getUser() != null )
        {
            return new UsernamePasswordCredentials( repo.getUser(), repo.getPassword() );
        }

        return null;
    }

}
