package org.commonjava.aprox.audit;

import java.security.Principal;
import java.security.PrivilegedAction;

public interface SecuritySystem
{

    String SYSTEM_USER = "system";

    /**
     * Throw {@link SecurityException} if no principal is registered in the current context.
     */
    Principal getCurrentPrincipal();

    boolean hasCurrentPrincipal();

    void clearCurrentPrincipal();

    <T, E extends Throwable, C extends SecurityAction<T, E>> T runAsSystemUser( C action );

    <T> T runAsSystemUser( PrivilegedAction<T> action );

    <T, E extends Throwable, C extends SecurityAction<T, E>> T runAsCurrentPrincipal( C action )
        throws SecuritySystemException;

    <T> T runAsCurrentPrincipal( PrivilegedAction<T> action )
        throws SecuritySystemException;

    <T, E extends Throwable, C extends SecurityAction<T, E>> T runAs( Principal principal, C action )
        throws SecuritySystemException;

    <T> T runAs( Principal principal, PrivilegedAction<T> action )
        throws SecuritySystemException;

    void setCurrentPrincipal( Principal principal );

}
