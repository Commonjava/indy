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
import org.commonjava.util.logging.Logger;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.SectionName;

@ApplicationScoped
@Named( AutoProxConfigurator.SECTION )
@SectionName( "autoprox" )
public class AutoProxConfigurator
    extends AbstractAproxMapConfig
{
    public static final String SECTION = "autoprox";

    private static final String APROX_ETC = System.getProperty( "aprox.etc", "/etc/aprox" );

    public static final File DEFAULT_DIR = new File( APROX_ETC, "autoprox.d" );

    public static final String DEFAULT_FACTORY_SCRIPT = "default.groovy";

    public static final String BASEDIR_PARAM = "basedir";

    public static final String ENABLED_PARAM = "enabled";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ScriptEngine scriptEngine;

    private File basedir;

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

    public void setBasedir( final File basedir )
    {
        this.basedir = basedir;
    }

    public File getBasedir()
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
        if ( BASEDIR_PARAM.equals( name ) )
        {
            basedir = new File( value );
        }
        else if ( ENABLED_PARAM.equals( name ) )
        {
            enabled = Boolean.parseBoolean( value );
        }
        else
        {
            factoryProtoMappings.put( name, value );
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
        return new AutoProxConfig( factoryMappings, isEnabled() );
    }

    private synchronized void buildFactories()
    {
        if ( factoryMappings == null )
        {
            factoryMappings = new ArrayList<FactoryMapping>();

            if ( factoryProtoMappings.isEmpty() )
            {
                final File script = new File( getBasedir(), DEFAULT_FACTORY_SCRIPT );
                if ( script.exists() )
                {
                    try
                    {
                        final AutoProxFactory factory = scriptEngine.parseScriptInstance( script, AutoProxFactory.class );
                        factoryMappings.add( new FactoryMapping( FactoryMapping.DEFAULT_MATCH, factory ) );
                    }
                    catch ( final AproxGroovyException e )
                    {
                        logger.error( "[AUTOPROX] Cannot load autoprox factory from: %s (matched via: '%s'). Reason: %s", e, script,
                                      FactoryMapping.DEFAULT_MATCH, e.getMessage() );
                    }
                }
            }
            else
            {
                for ( final Entry<String, String> entry : factoryProtoMappings.entrySet() )
                {
                    final String match = entry.getKey();
                    final String scriptPath = entry.getValue();

                    final File script = new File( getBasedir(), scriptPath );
                    if ( !script.exists() )
                    {
                        logger.error( "[AUTOPROX] Cannot load autoprox factory from: %s (matched via: '%s'). File does not exist.", script, match );
                        continue;
                    }

                    try
                    {
                        final AutoProxFactory factory = scriptEngine.parseScriptInstance( script, AutoProxFactory.class );
                        factoryMappings.add( new FactoryMapping( match, factory ) );
                    }
                    catch ( final AproxGroovyException e )
                    {
                        logger.error( "[AUTOPROX] Cannot load autoprox factory from: %s (matched via: '%s'). Reason: %s", e, script, match,
                                      e.getMessage() );
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
