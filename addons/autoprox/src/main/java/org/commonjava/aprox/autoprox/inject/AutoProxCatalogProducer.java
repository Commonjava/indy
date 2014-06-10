package org.commonjava.aprox.autoprox.inject;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.autoprox.conf.AutoProxConfig;
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.autoprox.conf.FactoryMapping;
import org.commonjava.aprox.autoprox.model.AutoProxCatalog;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AutoProxCatalogProducer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FlatFileConfiguration ffConfig;

    @Inject
    private AutoProxConfig apConfig;

    @Inject
    private ScriptEngine scriptEngine;

    private AutoProxCatalog catalog;

    protected AutoProxCatalogProducer()
    {
    }

    public AutoProxCatalogProducer( final FlatFileConfiguration ffConfig, final AutoProxConfig apConfig )
    {
        this.ffConfig = ffConfig;
        this.apConfig = apConfig;
        this.scriptEngine = new ScriptEngine();
        init();
    }

    @PostConstruct
    public void init()
    {
        if ( !apConfig.isEnabled() )
        {
            catalog = new AutoProxCatalog( false );
        }

        final File dataBasedir = ffConfig.getDataBasedir();
        final File dataDir = new File( dataBasedir, apConfig.getDataDir() );

        final List<FactoryMapping> factoryMappings = new ArrayList<FactoryMapping>();

        @SuppressWarnings( "deprecation" )
        final List<FactoryMapping> deprecatedMappings = apConfig.getFactoryMappings();
        if ( deprecatedMappings != null && !deprecatedMappings.isEmpty() )
        {
            factoryMappings.addAll( deprecatedMappings );
        }

        if ( dataDir.exists() )
        {
            final File[] scripts = dataDir.listFiles( new FileFilter()
            {
                @Override
                public boolean accept( final File pathname )
                {
                    return pathname.getName()
                                   .endsWith( ".groovy" );
                }
            } );

            for ( final File script : scripts )
            {
                try
                {
                    final AutoProxFactory factory = scriptEngine.parseScriptInstance( script, AutoProxFactory.class );
                    factoryMappings.add( new FactoryMapping( script.getName(), factory ) );
                }
                catch ( final AproxGroovyException e )
                {
                    logger.error( "[AUTOPROX] Cannot load autoprox factory from: {}. Reason: {}", e, script,
                                  e.getMessage() );
                }
            }
        }

        catalog = new AutoProxCatalog( true, factoryMappings );
    }

    @Produces
    @Production
    @Default
    public AutoProxCatalog getCatalog()
    {
        return catalog;
    }

}
