/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.autoprox.util;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.autoprox.data.AutoProxRule;
import org.commonjava.aprox.autoprox.data.AutoProxRuleException;
import org.commonjava.aprox.autoprox.data.RuleMapping;
import org.commonjava.aprox.subsys.datafile.DataFile;
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

    public RuleMapping parseRule( final DataFile script )
        throws AutoProxRuleException
    {
        String spec = null;
        try
        {
            spec = script.readString();
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "[AUTOPROX] Cannot load autoprox factory from: %s. Reason: %s", script,
                                         e.getMessage() ), e );
        }

        return parseRule( spec, script.getName() );
    }

    public RuleMapping parseRule( final File script )
        throws AutoProxRuleException
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

        return parseRule( spec, script.getName() );
    }

    public RuleMapping parseRule( final String spec, final String scriptName )
        throws AutoProxRuleException
    {
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
            throw new AutoProxRuleException(
                                             "[AUTOPROX] Cannot load autoprox factory from: {} as an instance of: {}. Reason: {}",
                                             e, scriptName, AutoProxRule.class.getSimpleName(),
                         e.getMessage() );
        }

        if ( rule != null )
        {
            return new RuleMapping( scriptName, spec, rule );
        }

        return null;
    }

}
