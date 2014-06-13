package org.commonjava.aprox.autoprox.util;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.autoprox.conf.AutoProxFactoryRuleAdapter;
import org.commonjava.aprox.autoprox.data.AutoProxRule;
import org.commonjava.aprox.autoprox.data.RuleMapping;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings( "deprecation" )
@ApplicationScoped
public class ScriptRuleParser
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ScriptEngine scriptEngine;

    protected ScriptRuleParser()
    {
    }

    public ScriptRuleParser( final ScriptEngine scriptEngine )
    {
        this.scriptEngine = scriptEngine;
    }

    public RuleMapping parseRule( final File script )
    {
        String spec = null;
        try
        {
            spec = FileUtils.readFileToString( script );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "[AUTOPROX] Cannot load autoprox factory from: %s. Reason: %s", script,
                                         e.getMessage() ), e );
        }

        if ( spec == null )
        {
            return null;
        }

        AutoProxRule rule = null;
        try
        {

            rule = scriptEngine.parseScriptInstance( spec, AutoProxRule.class );
        }
        catch ( final AproxGroovyException e )
        {
            logger.warn( "[AUTOPROX] Cannot load autoprox factory from: {} as an instance of: {}. Reason: {}\nTrying again with legacy interface: {}",
                         script, AutoProxRule.class.getSimpleName(), AutoProxFactory.class.getSimpleName(),
                         e.getMessage() );

            try
            {
                final AutoProxFactory factory = scriptEngine.parseScriptInstance( spec, AutoProxFactory.class );

                rule = new AutoProxFactoryRuleAdapter( factory );
            }
            catch ( final AproxGroovyException eInner )
            {
                logger.warn( String.format( "[AUTOPROX] Cannot load autoprox factory from: %s as an instance of: %s. Reason: %s",
                                            script, AutoProxFactory.class.getSimpleName(), eInner.getMessage() ),
                             eInner );
            }
        }

        if ( rule != null )
        {
            return new RuleMapping( script.getName(), spec, rule );
        }

        return null;
    }

}
