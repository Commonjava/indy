/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.model.galley;

import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;

/**
 * {@link KeyedLocation} implementation that represents an AProx {@link Group} store.
 */
public class GroupLocation
    extends CacheOnlyLocation
    implements KeyedLocation
{

    public GroupLocation( final String name )
    {
        super( new StoreKey( StoreType.group, name ) );
    }

    @Override
    public String toString()
    {
        return "GroupLocation [" + getKey() + "]";
    }
}
