package org.commonjava.aprox.audit;

import java.security.Principal;
import java.security.PrivilegedAction;

import org.apache.http.auth.BasicUserPrincipal;

public class BasicSecuritySystem
    implements SecuritySystem
{

    private final InheritableThreadLocal<Principal> principal = new InheritableThreadLocal<Principal>();

    @Override
    public Principal getCurrentPrincipal()
    {
        final Principal p = principal.get();
        if ( p == null )
        {
            throw new SecurityException( "Not logged in!" );
        }

        return p;
    }

    @Override
    public boolean hasCurrentPrincipal()
    {
        return principal.get() != null;
    }

    @Override
    public void clearCurrentPrincipal()
    {
        principal.remove();
    }

    @Override
    public <T> T runAsSystemUser( final PrivilegedAction<T> action )
    {
        return _runAs( new BasicUserPrincipal( SYSTEM_USER ), action );
    }

    @Override
    public <T, E extends Throwable, C extends SecurityAction<T, E>> T runAsSystemUser( final C action )
    {
        return _runAs( new BasicUserPrincipal( SYSTEM_USER ), action );
    }

    @Override
    public <T> T runAsCurrentPrincipal( final PrivilegedAction<T> action )
    {
        return _runAs( getCurrentPrincipal(), action );
    }

    @Override
    public <T, E extends Throwable, C extends SecurityAction<T, E>> T runAsCurrentPrincipal( final C action )
    {
        return _runAs( getCurrentPrincipal(), action );
    }

    @Override
    public <T, E extends Throwable, C extends SecurityAction<T, E>> T runAs( final Principal p, final C action )
        throws SecuritySystemException
    {
        return _runAs( p, action );
    }

    @Override
    public <T> T runAs( final Principal p, final PrivilegedAction<T> action )
        throws SecuritySystemException
    {
        return _runAs( p, action );
    }

    private <T> T _runAs( final Principal p, final PrivilegedAction<T> action )
    {
        final Principal old = principal.get();
        try
        {
            principal.set( p );
            return action.run();
        }
        finally
        {
            principal.set( old );
        }
    }

    @Override
    public void setCurrentPrincipal( final Principal principal )
    {
        this.principal.set( principal );
    }

}
