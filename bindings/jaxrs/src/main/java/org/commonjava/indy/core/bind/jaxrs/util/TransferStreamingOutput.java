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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.commonjava.cdi.util.weft.NamedThreadFactory;
import org.commonjava.indy.IndyMetricsNames;
import org.commonjava.indy.core.bind.jaxrs.metrics.IndyMetricsBindingsNames;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferStreamingOutput
    implements StreamingOutput
{
    /*
        In this TransferStreamOutput, we used a tweak way to fix a stream escape problem. Because we're using weft to manage all thread pool creation,
        and to manage all stream closing to indy client response, weft wrapped all threads runnable and intercept a single ThreadContext to all the child threads
        which are initiated by the parent thread, and register the stream closing action in that ThreadContext. Currently we met a problem like this:
        when a undertow initiated a request thread(we call it root thread) and hit the ResourceManagementFilter, in the filter method a root ThreadContext will
        be initiated and be propagated to all of the child threads which are initiated by this root thread. So that means all of these threads will share the stream
        closing actions(which are registered in Partyline FileManager). And something bad happened here: when this TransferStreamingOutput returned the output stream
        back to client, the reading action may not be finished(especially when some big file reading), but the stream may be closed by some other threads when these threads
        finished their jobs and call the finializers. Of course this does not make sense.
        So here we used this tweak way to fix: use split threads(which is not wrapped by the weft), and all of its context is not shared. So no other threads will
        hold the stream closing actions as their finializers here.
     */

    private final Transfer transfer;
    private final EventMetadata metadata;

    //TODO: maybe performance impacting to use this executor. And the priority 1 should not be very suitable
    private static final ExecutorService NONE_WEFT_EXECUTOR =
            Executors.newFixedThreadPool( 8, new NamedThreadFactory( "TSO-exec", false, 1 ) );

    public TransferStreamingOutput( final Transfer transfer, final EventMetadata metadata )
    {
        this.transfer = transfer;
        this.metadata = metadata;
        transfer.lockWrite();
    }

    @Override
    @IndyMetrics( measure = @Measure( meters = @MetricNamed( name =
                    IndyMetricsBindingsNames.METHOD_TRANSFERSTREAMING_WRITE
                                    + IndyMetricsNames.METER ), timers = @MetricNamed( name =
                    IndyMetricsBindingsNames.METHOD_TRANSFERSTREAMING_WRITE + IndyMetricsNames.TIMER ) ) )
    public void write( final OutputStream out )
            throws IOException, WebApplicationException
    {
        final Logger logger = LoggerFactory.getLogger( TransferStreamingOutput.class );
        final AtomicReference<IOException> exception = new AtomicReference<>();
        Future<InputStream> task = NONE_WEFT_EXECUTOR.submit( () -> {
            try
            {
                InputStream stream = transfer.openInputStream( true, metadata );
                logger.trace( "TSO: Stream from partyline {}, current thread: {}, current thread trace:\n{}", stream,
                             Thread.currentThread().getName(),
                             StringUtils.join( Thread.currentThread().getStackTrace(), "\n" ) );
                return stream;
            }
            catch ( IOException e )
            {
                exception.set( e );
                return null;
            }
        } );

        InputStream stream = null;
        try
        {
            stream = task.get();
            if ( stream != null )
            {
                CountingOutputStream cout = new CountingOutputStream( out );
                IOUtils.copy( stream, cout );

                logger.debug( "Wrote: {} bytes", cout.getByteCount() );
            }
        }
        catch ( InterruptedException | ExecutionException e )
        {
            throw new WebApplicationException( "Something wrong happened during get underline transfer stream" );
        }
        finally
        {
            if ( stream != null )
            {
                stream.close();
            }
            transfer.unlock();

        }

        if ( exception.get() != null )
        {
            throw exception.get();
        }
    }

}
