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
package org.commonjava.indy.test.fixture.core;

import javax.enterprise.inject.Instance;
import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 */
public class MockInstance<T> implements Instance<T>
{
    private Collection<T> elems;

    public MockInstance( T elem){
        this.elems = new ArrayList<>();
        elems.add( elem );
    }

    public MockInstance( Collection<T> elems )
    {
        this.elems = elems;
    }

    @Override
    public Instance<T> select( Annotation... qualifiers )
    {
        return null;
    }

    @Override
    public <U extends T> Instance<U> select( Class<U> subtype, Annotation... qualifiers )
    {
        return null;
    }

    @Override
    public <U extends T> Instance<U> select( TypeLiteral<U> subtype, Annotation... qualifiers )
    {
        return null;
    }

    @Override
    public boolean isUnsatisfied()
    {
        return false;
    }

    @Override
    public boolean isAmbiguous()
    {
        return false;
    }

    @Override
    public void destroy( T instance )
    {

    }

    @Override
    public Iterator<T> iterator()
    {
        return elems.iterator();
    }

    @Override
    public T get()
    {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize( elems.iterator(), Spliterator.ORDERED ), false )
                    .filter( Objects::nonNull )
                    .findFirst()
                    .orElse( null );
    }
}
