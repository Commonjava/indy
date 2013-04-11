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
