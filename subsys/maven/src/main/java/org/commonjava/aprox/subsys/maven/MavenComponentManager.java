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
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.maven.mae.MAEException;
import org.apache.maven.mae.boot.embed.MAEEmbedderBuilder;
import org.apache.maven.mae.conf.CoreLibrary;
import org.apache.maven.mae.conf.MAEConfiguration;
import org.apache.maven.mae.internal.container.ComponentSelector;
import org.apache.maven.mae.internal.container.MAEContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.LogEnabled;
import org.commonjava.aprox.subsys.maven.plogger.Log4JLoggerManager;

@ApplicationScoped
public class MavenComponentManager
{

    @Inject
    private Instance<MavenComponentDefinitions> componentLists;

    private MAEContainer container;

    private Log4JLoggerManager loggerManager;

    @PostConstruct
    public void startMAE()
        throws MAEException
    {
        final ComponentSelector selector = new ComponentSelector();

        for ( final MavenComponentDefinitions list : componentLists )
        {
            for ( final MavenComponentDefinition<?, ?> comp : list )
            {
                final String oldHint = comp.getOverriddenHint();
                if ( oldHint == null )
                {
                    selector.setSelection( comp.getComponentClass(), comp.getHint() );
                }
                else
                {
                    selector.setSelection( comp.getComponentClass(), oldHint, comp.getHint() );
                }
            }
        }

        final MAEConfiguration config = new MAEConfiguration().withComponentSelections( selector )
                                                              .withLibrary( new CoreLibrary() );

        final MAEEmbedderBuilder builder = new MAEEmbedderBuilder().withConfiguration( config );

        container = builder.container();
        loggerManager = new Log4JLoggerManager();

        container.setLoggerManager( loggerManager );
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
