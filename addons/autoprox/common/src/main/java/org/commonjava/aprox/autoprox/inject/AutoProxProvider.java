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
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;

@ApplicationScoped
public class AutoProxProvider
{

    @Inject
    private FlatFileConfiguration ffConfig;

    @Inject
    private AutoProxConfig apConfig;

    @Inject
    private ScriptRuleParser ruleParser;

    private AutoProxCatalog catalog;

    protected AutoProxProvider()
    {
    }

    public AutoProxProvider( final FlatFileConfiguration ffConfig, final AutoProxConfig apConfig,
                             final ScriptRuleParser ruleParser )
    {
        this.ffConfig = ffConfig;
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

        final File dataBasedir = ffConfig.getDataBasedir();
        final File dataDir = new File( dataBasedir, apConfig.getDataDir() );

        final List<RuleMapping> ruleMappings = new ArrayList<RuleMapping>();

        @SuppressWarnings( "deprecation" )
        final List<RuleMapping> deprecatedMappings = apConfig.getRuleMappings();
        if ( deprecatedMappings != null && !deprecatedMappings.isEmpty() )
        {
            ruleMappings.addAll( deprecatedMappings );
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
