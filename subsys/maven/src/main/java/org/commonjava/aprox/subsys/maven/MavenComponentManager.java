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
            throw new MavenComponentException( "Failed to lookup maven component: %s:%s. Reason: %s", e,
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
            throw new MavenComponentException( "Failed to lookup maven component: %s. Reason: %s", e, role.getName(),
                                               e.getMessage() );
        }
    }

}
