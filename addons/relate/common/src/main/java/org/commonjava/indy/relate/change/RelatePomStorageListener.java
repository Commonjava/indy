/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.relate.change;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.relate.conf.RelateConfig;
import org.commonjava.indy.relate.util.RelateGenerationManager;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static org.commonjava.indy.relate.util.RelateGenerationManager.REL_DIRECT_GENERATING;

/**
 * This listener tracks file storage events. If a pom download occurs it ensures that associated .rel is generated.
 * This also triggers on POM uploads for HostedRepository storage.
 *
 * @author ruhan
 */
@ApplicationScoped
public class RelatePomStorageListener
{
    @Inject
    private RelateConfig config;

    @Inject
    private RelateGenerationManager relateGenerationManager;

    @WeftManaged
    @Inject
    @ExecutorConfig( named = "relate-pool" )
    private ExecutorService executor;

    public void onPomStorage( @Observes final FileStorageEvent event )
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );

        if ( !config.isEnabled() )
        {
            logger.debug( "Relate Add-on is not enabled." );
            return;
        }

        logger.debug( "FILE STORAGE: {}", event );

        final TransferOperation op = event.getType();
        if ( op != TransferOperation.UPLOAD && op != TransferOperation.DOWNLOAD )
        {
            logger.debug( "Not a download/upload transfer operation. No .rel generation." );
            return;
        }

        ThreadContext threadContext = ThreadContext.getContext( false );
        if ( threadContext != null )
        {
            Object obj = threadContext.get( REL_DIRECT_GENERATING );
            if ( obj != null )
            {
                logger.debug( "Direct .rel generation in progress. Ignore POM storage event. " );
                return;
            }
        }
        final Transfer transfer = event.getTransfer();
        executor.execute( () ->
                          {
                              relateGenerationManager.generateRelationshipFile( transfer, op );
                          } );
    }
}
