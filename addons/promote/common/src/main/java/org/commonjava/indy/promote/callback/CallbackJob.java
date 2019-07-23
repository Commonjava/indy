/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
