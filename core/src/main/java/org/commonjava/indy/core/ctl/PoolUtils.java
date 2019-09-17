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
