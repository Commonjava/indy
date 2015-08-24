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
package org.commonjava.aprox.client.core;

import com.fasterxml.jackson.databind.Module;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;

import java.util.Collections;

public abstract class AproxClientModule
{

    protected AproxClientHttp http;

    protected Aprox client;

    protected void setup( final Aprox client, final AproxClientHttp http )
    {
        this.client = client;
        this.http = http;
    }

    public Iterable<Module> getSerializerModules()
    {
        return Collections.emptySet();
    }

    protected Aprox getClient()
    {
        return client;
    }

    protected AproxClientHttp getHttp()
    {
        return http;
    }

    protected AproxObjectMapper getObjectMapper()
    {
        return http.getObjectMapper();
    }

    @Override
    public final int hashCode()
    {
        return 13 * getClass().hashCode();
    }

    @Override
    public final boolean equals( final Object other )
    {
        return other == this || getClass().equals( other.getClass() );
    }

}
