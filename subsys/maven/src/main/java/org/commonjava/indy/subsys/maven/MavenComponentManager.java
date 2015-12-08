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
package org.commonjava.indy.subsys.maven;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.commonjava.indy.subsys.maven.plogger.Log4JLoggerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MavenComponentManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private PlexusContainer container;

    private Log4JLoggerManager loggerManager;

    @PostConstruct
    public void startMAE()
    {
        final DefaultContainerConfiguration cc = new DefaultContainerConfiguration();

        loggerManager = new Log4JLoggerManager();

        try
        {
            container = new DefaultPlexusContainer( cc );
        }
        catch ( final PlexusContainerException e )
        {
            logger.error( String.format( "Cannot start plexus container for access to Maven components: %s",
                                         e.getMessage() ), e );
        }
    }

    public <I> I getComponent( final Class<I> role, final String hint )
        throws MavenComponentException
    {
        try
        {
            final I instance = container.lookup( role, hint );
            if ( instance instanceof LogEnabled )
            {
                final org.codehaus.plexus.logging.Logger log =
                    loggerManager.getLoggerForComponent( role.getName(), hint );

                ( (LogEnabled) instance ).enableLogging( log );
            }

            return instance;
        }
        catch ( final ComponentLookupException e )
        {
            throw new MavenComponentException( "Failed to lookup maven component: {}:{}. Reason: {}", e,
                                               role.getName(), hint, e.getMessage() );
        }
    }

    public <I> I getComponent( final Class<I> role )
        throws MavenComponentException
    {
        try
        {
            final I instance = container.lookup( role );
            if ( instance instanceof LogEnabled )
            {
                final org.codehaus.plexus.logging.Logger log = loggerManager.getLoggerForComponent( role.getName() );

                ( (LogEnabled) instance ).enableLogging( log );
            }

            return instance;
        }
        catch ( final ComponentLookupException e )
        {
            throw new MavenComponentException( "Failed to lookup maven component: {}. Reason: {}", e, role.getName(),
                                               e.getMessage() );
        }
    }

}
