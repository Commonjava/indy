package org.commonjava.indy.promote.callback;

import org.commonjava.indy.promote.model.AbstractPromoteResult;
import org.commonjava.indy.promote.model.CallbackTarget;

import java.util.UUID;

/**
 * Created by ruhan on 1/10/19.
 */
class CallbackJob<T extends AbstractPromoteResult>
{
    public CallbackJob( CallbackTarget target, T ret )
    {
        this.id = UUID.randomUUID().toString();
        this.target = target;
        this.ret = ret;
    }

    private String id;

    private CallbackTarget target;

    private T ret;

    private int retryCount;

    public String getId()
    {
        return id;
    }

    public CallbackTarget getTarget()
    {
        return target;
    }

    public T getRet()
    {
        return ret;
    }

    public int getRetryCount()
    {
        return retryCount;
    }

    public void increaseRetryCount()
    {
        retryCount++;
    }
}
