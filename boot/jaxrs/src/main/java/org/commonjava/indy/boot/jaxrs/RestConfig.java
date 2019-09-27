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
package org.commonjava.indy.boot.jaxrs;

import io.undertow.Undertow;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.max;

@SectionName( "rest")
@ApplicationScoped
public class RestConfig
        implements IndyConfigInfo
{
    private Integer ioThreads;

    private Integer workerThreads;

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "rest.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-rest.conf" );
    }

    public Integer getIoThreads()
    {
        return ioThreads;
    }

    @ConfigName( "io.threads" )
    public void setIoThreads( final Integer ioThreads )
    {
        this.ioThreads = ioThreads;
    }

    public Integer getWorkerThreads()
    {
        return workerThreads;
    }

    @ConfigName( "worker.threads" )
    public void setWorkerThreads( final Integer workerThreads )
    {
        this.workerThreads = workerThreads;
    }

    public void configureBuilder( final Undertow.Builder builder ) throws IOException
    {
        int ioThreads = this.ioThreads == null ? max( Runtime.getRuntime().availableProcessors(), 2 ) : this.ioThreads;
        int workerThreads = this.workerThreads == null ? ioThreads * 8 : this.workerThreads;

        Xnio xnio = Xnio.getInstance( Undertow.class.getClassLoader() );
        final OptionMap.Builder omBuilder = OptionMap.builder()
                                                     .set( Options.WORKER_IO_THREADS, ioThreads )
                                                     .set( Options.CONNECTION_HIGH_WATER, 2000000 )
                                                     .set( Options.CONNECTION_LOW_WATER, 1000000 )
                                                     .set( Options.TCP_NODELAY, true )
                                                     .set( Options.CORK, true )
                                                     .set( Options.WORKER_TASK_CORE_THREADS, workerThreads )
                                                     .set( Options.WORKER_TASK_MAX_THREADS, workerThreads )
                                                    .set(Options.KEEP_ALIVE, false)
                                                    .set(Options.WORKER_TASK_KEEPALIVE, 60000)
            ;

        final OptionMap optionMap = omBuilder.getMap();
        builder.setWorker( xnio.createWorker( new ThreadGroup( "REST" ), optionMap ) );
    }
}
