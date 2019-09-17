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
package org.commonjava.indy.httprox.util;

import org.xnio.ChannelListener;
import org.xnio.Option;
import org.xnio.XnioExecutor;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruhan on 9/19/18.
 */
public class OutputStreamSinkChannel
                implements StreamSinkChannel
{
    private final OutputStream outputStream;

    public OutputStreamSinkChannel( OutputStream outputStream )
    {
        this.outputStream = outputStream;
    }

    @Override
    public long transferFrom( FileChannel fileChannel, long l, long l1 ) throws IOException
    {
        return 0;
    }

    @Override
    public long transferFrom( StreamSourceChannel streamSourceChannel, long l, ByteBuffer byteBuffer )
                    throws IOException
    {
        return 0;
    }

    @Override
    public void suspendWrites()
    {

    }

    @Override
    public void resumeWrites()
    {

    }

    @Override
    public boolean isWriteResumed()
    {
        return false;
    }

    @Override
    public void wakeupWrites()
    {

    }

    @Override
    public void shutdownWrites() throws IOException
    {

    }

    @Override
    public void awaitWritable() throws IOException
    {

    }

    @Override
    public void awaitWritable( long l, TimeUnit timeUnit ) throws IOException
    {

    }

    @Override
    public XnioExecutor getWriteThread()
    {
        return null;
    }

    @Override
    public ChannelListener.Setter<? extends StreamSinkChannel> getWriteSetter()
    {
        return null;
    }

    @Override
    public ChannelListener.Setter<? extends StreamSinkChannel> getCloseSetter()
    {
        return null;
    }

    @Override
    public XnioWorker getWorker()
    {
        return null;
    }

    @Override
    public XnioIoThread getIoThread()
    {
        return null;
    }

    @Override
    public boolean flush() throws IOException
    {
        outputStream.flush();
        return true;
    }

    @Override
    public int writeFinal( ByteBuffer byteBuffer ) throws IOException
    {
        return 0;
    }

    @Override
    public long writeFinal( ByteBuffer[] byteBuffers, int i, int i1 ) throws IOException
    {
        return 0;
    }

    @Override
    public long writeFinal( ByteBuffer[] byteBuffers ) throws IOException
    {
        return 0;
    }

    @Override
    public long write( ByteBuffer[] byteBuffers, int i, int i1 ) throws IOException
    {
        return 0;
    }

    @Override
    public long write( ByteBuffer[] byteBuffers ) throws IOException
    {
        return 0;
    }

    @Override
    public int write( ByteBuffer byteBuffer ) throws IOException
    {
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        outputStream.write( bytes );
        return bytes.length;
    }

    @Override
    public boolean isOpen()
    {
        return true;
    }

    @Override
    public void close() throws IOException
    {

    }

    @Override
    public boolean supportsOption( Option<?> option )
    {
        return false;
    }

    @Override
    public <T> T getOption( Option<T> option ) throws IOException
    {
        return null;
    }

    @Override
    public <T> T setOption( Option<T> option, T t ) throws IllegalArgumentException, IOException
    {
        return null;
    }
}
