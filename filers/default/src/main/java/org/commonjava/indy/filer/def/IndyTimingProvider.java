/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.filer.def;

import org.commonjava.maven.galley.spi.metrics.TimingProvider;

public class IndyTimingProvider
        implements TimingProvider
{
    private final String name;

    private long startNanos;

    public IndyTimingProvider( final String name )
    {
        this.name = name;
    }

    @Override
    public void start( final String name )
    {
        startNanos = System.nanoTime();
    }

    @Override
    public long stop()
    {
        return System.nanoTime() - startNanos;
    }
}
