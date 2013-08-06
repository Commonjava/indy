/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.depgraph;

import java.util.concurrent.ExecutorService;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.discover.AproxModelDiscoverer;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class DepgraphStorageListener
{

    @Inject
    private AproxModelDiscoverer discoverer;

    @Inject
    @ExecutorConfig( priority = 8, threads = 2, named = "tensor-listener" )
    private ExecutorService executor;

    private final Logger logger = new Logger( getClass() );

    public void handleFileAccessEvent( @Observes final FileAccessEvent event )
    {
        final String path = event.getTransfer()
                                 .getPath();

        if ( !path.endsWith( ".pom" ) )
        {
            return;
        }

        logger.info( "[SUBMIT] TensorStorageListenerRunnable for: %s", event );

        executor.execute( new DepgraphStorageListenerRunnable( discoverer, event.getTransfer() ) );
    }
}
