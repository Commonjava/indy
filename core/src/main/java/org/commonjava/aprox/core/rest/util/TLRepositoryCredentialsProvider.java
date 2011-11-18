/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
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
