/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.discover.AproxModelDiscoverer;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class DepgraphStorageListener
{

    @Inject
    private AproxModelDiscoverer discoverer;

    @Inject
    private StoreDataManager aprox;

    //    @Inject
    //    @ExecutorConfig( priority = 8, threads = 2, named = "depgraph-listener" )
    //    private ExecutorService executor;

    private final Logger logger = new Logger( getClass() );

    public void handleFileAccessEvent( @Observes final FileAccessEvent event )
    {
        //        final String path = event.getTransfer()
        //                                 .getPath();
        //
        //        if ( !path.endsWith( ".pom" ) )
        //        {
        //            return;
        //        }
        //
        //        logger.info( "[SUBMIT] DepgraphStorageListenerRunnable for: %s", event );
        //
        //        executor.execute( new DepgraphStorageListenerRunnable( discoverer, aprox, event.getTransfer() ) );
    }
}
