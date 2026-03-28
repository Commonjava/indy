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
package org.commonjava.indy.core.bind.jaxrs.util;

import org.apache.commons.io.input.CountingInputStream;
import org.commonjava.maven.galley.util.IdempotentCloseInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class TransferCountingInputStream
        extends IdempotentCloseInputStream
{

    private long size;

    public TransferCountingInputStream( final InputStream stream )
    {
        super( new CountingInputStream( stream ) );
    }

    @Override
    public void close()
            throws IOException
    {
        try
        {
            CountingInputStream stream = (CountingInputStream) this.in;
            Logger logger = LoggerFactory.getLogger( getClass() );
            size = stream.getByteCount();
            logger.trace( "Reads: {} bytes", size );
        }
        finally
        {
            super.close();
        }

    }

    long getSize()
    {
        return size;
    }
}
