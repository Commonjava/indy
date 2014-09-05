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
import org.commonjava.aprox.autoprox.data.AutoProxCatalog;
import org.commonjava.aprox.autoprox.data.RuleMapping;
import org.commonjava.aprox.autoprox.util.ScriptRuleParser;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFile;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AutoProxProvider
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FlatFileManager ffManager;

    @Inject
    private AutoProxConfig apConfig;

    @Inject
    private ScriptRuleParser ruleParser;

    private AutoProxCatalog catalog;

    protected AutoProxProvider()
    {
    }

    public AutoProxProvider( final FlatFileManager ffManager, final AutoProxConfig apConfig,
                             final ScriptRuleParser ruleParser )
    {
        this.ffManager = ffManager;
        this.apConfig = apConfig;
        this.ruleParser = ruleParser;
        init();
    }

    @PostConstruct
    public void init()
    {
        if ( !apConfig.isEnabled() )
        {
            catalog = new AutoProxCatalog( false );
        }


        final List<RuleMapping> ruleMappings = new ArrayList<RuleMapping>();

        @SuppressWarnings( "deprecation" )
        final List<RuleMapping> deprecatedMappings = apConfig.getRuleMappings();
        if ( deprecatedMappings != null && !deprecatedMappings.isEmpty() )
        {
            ruleMappings.addAll( deprecatedMappings );
        }

        final FlatFile dataDir = ffManager.getDataFile( apConfig.getDataDir() );
        if ( dataDir.exists() )
        {
            final FlatFile[] scripts = dataDir.listFiles( new FileFilter()
            {
                @Override
                public boolean accept( final File pathname )
                {
                    return pathname.getName()
                                   .endsWith( ".groovy" );
                }
            } );

            for ( final FlatFile script : scripts )
            {
                logger.info( "Reading autoprox rule from: {}", script );
                final RuleMapping rule = ruleParser.parseRule( script );
                if ( rule != null )
                {
                    ruleMappings.add( rule );
                }
            }
        }

        catalog = new AutoProxCatalog( true, ruleMappings );
    }

    @Produces
    @Production
    @Default
    public AutoProxCatalog getCatalog()
    {
        return catalog;
    }

}
