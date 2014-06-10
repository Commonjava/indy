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
package org.commonjava.aprox.autoprox.conf;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.ScriptEngine;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.SectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named( AutoProxConfigurator.SECTION )
@SectionName( "autoprox" )
public class AutoProxConfigurator
    extends AbstractAproxMapConfig
{
    public static final String SECTION = "autoprox";

    public static final String DEFAULT_DIR = "autoprox";

    public static final String BASEDIR_PARAM = "basedir";

    public static final String ENABLED_PARAM = "enabled";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ScriptEngine scriptEngine;

    private String basedir;

    private boolean enabled;

    private final LinkedHashMap<String, String> factoryProtoMappings = new LinkedHashMap<String, String>();

    private ArrayList<FactoryMapping> factoryMappings;

    public AutoProxConfigurator()
    {
        super( SECTION );
    }

    public AutoProxConfigurator( final ScriptEngine scriptEngine )
    {
        super( SECTION );
        this.scriptEngine = scriptEngine;
    }

    public void setBasedir( final String basedir )
    {
        this.basedir = basedir;
    }

    public String getBasedir()
    {
        return basedir == null ? DEFAULT_DIR : basedir;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    @Override
    public void parameter( final String name, final String value )
        throws ConfigurationException
    {
        logger.info( "[AUTOPROX] config: '{}' = '{}'", name, value );
        if ( BASEDIR_PARAM.equals( name ) )
        {
            basedir = value;
        }
        else if ( ENABLED_PARAM.equals( name ) )
        {
            enabled = Boolean.parseBoolean( value );
        }
        else
        {
            factoryProtoMappings.put( name, value );
        }

        if ( !factoryProtoMappings.isEmpty() )
        {
            logger.warn( "[DEPRECATION] autoprox.d style configuration is deprecated! You will not be able to edit autoprox rules via the UI for this configuration!" );
        }
    }

    @Override
    public void sectionStarted( final String name )
        throws ConfigurationException
    {
        // NOP; just block map init in the underlying implementation.
    }

    @Produces
    @Production
    @Default
    public AutoProxConfig getConfig()
    {
        buildFactories();
        return new AutoProxConfig( getBasedir(), isEnabled(), factoryMappings );
    }

    private synchronized void buildFactories()
    {
        if ( factoryMappings == null )
        {
            if ( factoryProtoMappings.isEmpty() )
            {
                return;
            }

            factoryMappings = new ArrayList<FactoryMapping>();

            for ( final Entry<String, String> entry : factoryProtoMappings.entrySet() )
            {
                final String match = entry.getKey();
                final String scriptPath = entry.getValue();

                final File script = new File( getBasedir(), scriptPath );
                if ( !script.exists() )
                {
                    logger.error( "[AUTOPROX] Cannot load autoprox factory from: {} (matched via: '{}'). File does not exist.",
                                  script, match );
                    continue;
                }

                try
                {
                    final AutoProxFactory factory = scriptEngine.parseScriptInstance( script, AutoProxFactory.class );
                    factoryMappings.add( new FactoryMapping( script.getName(), match, factory ) );
                }
                catch ( final AproxGroovyException e )
                {
                    logger.error( "[AUTOPROX] Cannot load autoprox factory from: {} (matched via: '{}'). Reason: {}",
                                  e, script, match, e.getMessage() );
                }
            }

            if ( factoryProtoMappings.isEmpty() || !factoryProtoMappings.containsKey( FactoryMapping.DEFAULT_MATCH ) )
            {
                File script = new File( getBasedir(), AutoProxFactory.LEGACY_FACTORY_NAME );
                if ( !script.exists() )
                {
                    script = new File( getBasedir(), AutoProxFactory.DEFAULT_FACTORY_SCRIPT );
                }

                if ( script.exists() )
                {
                    try
                    {
                        final AutoProxFactory factory =
                            scriptEngine.parseScriptInstance( script, AutoProxFactory.class );
                        factoryMappings.add( new FactoryMapping( script.getName(), FactoryMapping.DEFAULT_MATCH,
                                                                 factory ) );
                    }
                    catch ( final AproxGroovyException e )
                    {
                        logger.error( "[AUTOPROX] Cannot load autoprox factory from: {} (matched via: '{}'). Reason: {}",
                                      e, script, FactoryMapping.DEFAULT_MATCH, e.getMessage() );
                    }
                }
            }
        }

        if ( factoryMappings.isEmpty() )
        {
            enabled = false;
        }
    }

}
