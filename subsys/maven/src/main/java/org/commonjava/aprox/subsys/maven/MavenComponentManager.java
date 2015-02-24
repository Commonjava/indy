/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.subsys.maven;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.commonjava.aprox.subsys.maven.plogger.Log4JLoggerManager;
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
