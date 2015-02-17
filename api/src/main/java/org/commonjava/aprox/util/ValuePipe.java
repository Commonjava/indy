package org.commonjava.aprox.util;

public class ValuePipe<T>
{

    private T value;

    public boolean isFilled()
    {
        return value != null;
    }

    public boolean isEmpty()
    {
        return value == null;
    }

    public synchronized T get()
    {
        return value;
    }

    public synchronized void set( final T value )
    {
        this.value = value;
        this.notifyAll();
    }

}
