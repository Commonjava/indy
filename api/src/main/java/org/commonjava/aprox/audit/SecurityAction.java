package org.commonjava.aprox.audit;

import java.security.PrivilegedAction;

public interface SecurityAction<T, E extends Throwable>
    extends PrivilegedAction<T>
{

    public E getError();

}
