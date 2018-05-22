/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.subsys.prefetch;

import org.commonjava.maven.galley.model.ConcreteResource;

public class PrioritizedResource
        implements Comparable
{
    private ConcreteResource resource;

    private int priority;

    public PrioritizedResource( final ConcreteResource resource, final int priority )
    {
        this.resource = resource;
        this.priority = priority;
    }

    public ConcreteResource getResource()
    {
        return resource;
    }

    public void setResource( ConcreteResource resource )
    {
        this.resource = resource;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority( int priority )
    {
        this.priority = priority;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( resource == null ) ? 0 : resource.hashCode() );
        result = prime * result + priority;
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( !( obj instanceof PrioritizedResource ) )
        {
            return false;
        }
        final PrioritizedResource other = (PrioritizedResource) obj;
        if ( resource == null )
        {
            if ( other.resource != null )
            {
                return false;
            }
        }
        else if ( !resource.equals( other.resource ) )
        {
            return false;
        }

        return this.priority == other.priority;
    }

    @Override
    public int compareTo( Object o )
    {
        if ( o instanceof PrioritizedResource )
        {
            return ( (PrioritizedResource) o ).priority - this.priority;
        }
        else
        {
            return 1;
        }
    }

    @Override
    public String toString()
    {
        return resource.toString() + ", priority: " + priority;
    }
}
