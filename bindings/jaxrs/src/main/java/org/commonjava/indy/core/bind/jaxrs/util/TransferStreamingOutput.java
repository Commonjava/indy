/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.IndyMetricsNames;
import org.commonjava.indy.core.bind.jaxrs.metrics.IndyMetricsBindingsNames;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferStreamingOutput
    implements StreamingOutput
{

    private final Transfer transfer;

    public TransferStreamingOutput( final Transfer transfer )
    {
        this.transfer = transfer;
    }

    @Override
    @IndyMetrics( measure = @Measure( meters = @MetricNamed( name =
                    IndyMetricsBindingsNames.METHOD_TRANSFERSTREAMING_WRITE
                                    + IndyMetricsNames.METER ), timers = @MetricNamed( name =
                    IndyMetricsBindingsNames.METHOD_TRANSFERSTREAMING_WRITE + IndyMetricsNames.TIMER ) ) )
    public void write( final OutputStream out )
            throws IOException, WebApplicationException
    {
        synchronized ( transfer )
        {
            try (InputStream stream = transfer.openInputStream())
            {
                CountingOutputStream cout = new CountingOutputStream( out );
                IOUtils.copy( stream, cout );

                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.debug( "Wrote: {} bytes", cout.getByteCount() );
            }
        }
    }

}
