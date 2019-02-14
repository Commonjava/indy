package org.commonjava.indy.core.ctl;

import org.commonjava.cdi.util.weft.exception.PoolOverloadException;
import org.commonjava.indy.IndyWorkflowException;

public class PoolUtils
{
    public static <T> T detectOverload( PoolLoadingFunction<T> func )
            throws IndyWorkflowException
    {
        try
        {
            return func.load();
        }
        catch ( PoolOverloadException e )
        {
            throw new IndyWorkflowException( 409, e.getPoolName() + " Threadpool Overloaded (currentLoadFactor="
                    + e.getLoadFactor() + ", maxLoadFactor=" + e.getMaxLoadFactor() + ", tasks=" + e.getCurrentLoad()
                    + ", threads=" + e.getThreadCount() + ")" );
        }
    }

    public static void detectOverloadVoid( PoolLoadingVoidFunction func )
            throws IndyWorkflowException
    {
        try
        {
            func.load();
        }
        catch ( PoolOverloadException e )
        {
            throw new IndyWorkflowException( 409, e.getPoolName() + " Threadpool Overloaded (currentLoadFactor="
                    + e.getLoadFactor() + ", maxLoadFactor=" + e.getMaxLoadFactor() + ", tasks=" + e.getCurrentLoad()
                    + ", threads=" + e.getThreadCount() + ")" );
        }
    }

    @FunctionalInterface
    public interface PoolLoadingFunction<T>
    {
        T load() throws IndyWorkflowException;
    }

    @FunctionalInterface
    public interface PoolLoadingVoidFunction
    {
        void load() throws IndyWorkflowException;
    }
}
