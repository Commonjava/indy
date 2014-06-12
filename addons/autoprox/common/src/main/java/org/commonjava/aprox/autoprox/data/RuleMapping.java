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
package org.commonjava.aprox.autoprox.data;

public class RuleMapping
{

    public static final String DEFAULT_MATCH = "default";

    private final String match;

    private final AutoProxRule rule;

    private final String scriptName;

    private final String spec;

    public RuleMapping( final String scriptName, final String match, final String spec, final AutoProxRule factory )
    {
        this.scriptName = scriptName;
        this.match = match;
        this.spec = spec;
        this.rule = factory;
    }

    public RuleMapping( final String scriptName, final String spec, final AutoProxRule factory )
    {
        this.scriptName = scriptName;
        this.spec = spec;
        this.rule = factory;
        this.match = null;
    }

    public RuleMapping( final String match, final RuleMapping ruleMapping )
    {
        this.scriptName = ruleMapping.getScriptName();
        this.match = match;
        this.rule = ruleMapping.getRule();
        this.spec = ruleMapping.getSpecification();
    }

    public String getScriptName()
    {
        return scriptName;
    }

    public String getMatch()
    {
        return match;
    }

    public AutoProxRule getRule()
    {
        return rule;
    }

    public boolean matchesName( final String name )
    {
        if ( match != null )
        {
            if ( match.length() > 2 && match.charAt( 0 ) == '/' && match.charAt( match.length() - 1 ) == '/' )
            {
                return name.matches( match.substring( 1, match.length() - 1 ) );
            }
            else if ( match.endsWith( "*" ) )
            {
                if ( match.length() == 1 )
                {
                    return true;
                }
                else
                {
                    return name.startsWith( match.substring( 0, match.length() - 1 ) );
                }
            }
            else if ( DEFAULT_MATCH.equalsIgnoreCase( match ) )
            {
                return true;
            }
        }
        else
        {
            return rule.matches( name );
        }

        return false;
    }

    public String getSpecification()
    {
        return spec;
    }

}
