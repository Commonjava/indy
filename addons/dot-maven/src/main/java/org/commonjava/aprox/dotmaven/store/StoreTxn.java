package org.commonjava.aprox.dotmaven.store;

import java.security.Principal;

import net.sf.webdav.ITransaction;

public class StoreTxn
    implements ITransaction
{

    private final Principal principal;

    public StoreTxn( final Principal principal )
    {
        this.principal = principal;
    }

    @Override
    public Principal getPrincipal()
    {
        return principal;
    }

    @Override
    public String toString()
    {
        return String.format( "DotMavenTxn [principal=%s]", principal );
    }

}
