package org.commonjava.aprox.autoprox.util;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.autoprox.data.AutoProxRule;
import org.commonjava.aprox.autoprox.data.RuleMapping;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try
        {
            final String spec = FileUtils.readFileToString( script );

            final AutoProxRule factory = scriptEngine.parseScriptInstance( spec, AutoProxRule.class );

            return new RuleMapping( script.getName(), spec, factory );
        }
        catch ( final AproxGroovyException e )
        {
            logger.error( "[AUTOPROX] Cannot load autoprox factory from: {}. Reason: {}", e, script, e.getMessage() );
        }
        catch ( final IOException e )
        {
            logger.error( "[AUTOPROX] Cannot load autoprox factory from: {}. Reason: {}", e, script, e.getMessage() );
        }

        return null;
    }

}
