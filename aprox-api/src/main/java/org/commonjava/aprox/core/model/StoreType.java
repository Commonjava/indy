package org.commonjava.aprox.core.model;

public enum StoreType
{
    group( false ), repository( false ), deploy_point( true );

    private boolean writable;

    private StoreType( final boolean writable )
    {
        this.writable = writable;
    }

    public boolean isWritable()
    {
        return writable;
    }
}