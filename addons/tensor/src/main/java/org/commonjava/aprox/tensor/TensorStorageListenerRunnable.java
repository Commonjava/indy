/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.tensor;

import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.tensor.discover.AproxModelDiscoverer;
import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.discover.DiscoveryResult;

public class TensorStorageListenerRunnable
    implements Runnable
{

    private final StorageItem item;

    private DiscoveryResult result;

    private final AproxModelDiscoverer discoverer;

    private TensorDataException error;

    public TensorStorageListenerRunnable( final AproxModelDiscoverer discoverer, final StorageItem item )
    {
        this.discoverer = discoverer;
        this.item = item;
    }

    public DiscoveryResult getResult()
    {
        return result;
    }

    @Override
    public void run()
    {
        try
        {
            result = discoverer.discoverRelationships( item );
        }
        catch ( final TensorDataException e )
        {
            error = e;
        }
    }

    public DiscoveryResult getDiscoveryResult()
    {
        return result;
    }

    public TensorDataException getError()
    {
        return error;
    }
}
