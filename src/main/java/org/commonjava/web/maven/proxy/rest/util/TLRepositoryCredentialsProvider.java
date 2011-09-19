package org.commonjava.web.maven.proxy.rest.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.commonjava.web.maven.proxy.model.Repository;

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
