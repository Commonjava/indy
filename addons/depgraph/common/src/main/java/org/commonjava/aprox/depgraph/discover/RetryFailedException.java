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
package org.commonjava.aprox.depgraph.discover;

import java.util.List;

import org.commonjava.maven.cartographer.data.CartoDataException;

public class RetryFailedException
    extends CartoDataException
{

    private static final long serialVersionUID = 1L;

    public RetryFailedException( final String message, final List<Throwable> nested, final Object... params )
    {
        super( message, nested, params );
    }

    public RetryFailedException( final String message, final Object... params )
    {
        super( message, params );
    }

    public RetryFailedException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

}
