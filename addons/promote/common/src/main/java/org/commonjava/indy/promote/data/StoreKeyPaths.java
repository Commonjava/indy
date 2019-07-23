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
package org.commonjava.indy.promote.data;

import org.commonjava.indy.model.core.StoreKey;

import java.util.Objects;
import java.util.Set;

public class StoreKeyPaths
{
    final StoreKey target;

    final Set<String> paths;

    public StoreKeyPaths( StoreKey target, Set<String> paths )
    {
        this.target = target;
        this.paths = paths;
    }

    public StoreKey getTarget()
    {
        return target;
    }

    public Set<String> getPaths()
    {
        return paths;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        StoreKeyPaths that = (StoreKeyPaths) o;
        return target.equals( that.target ) && Objects.equals( paths, that.paths );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( target, paths );
    }

    @Override
    public String toString()
    {
        return "PathsLockKey{" + "target=" + target + ", paths=" + paths + '}';
    }
}
