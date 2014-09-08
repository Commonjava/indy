package org.commonjava.aprox.audit;

public abstract class SimpleSecurityAction<T, E extends Throwable>
    implements SecurityAction<T, E>
{

    private E error;

    protected final void setError( final E error )
    {
        this.error = error;
    }

    @Override
    public final E getError()
    {
        return error;
    }

    @Override
    public abstract T run();

}
