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

import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;

public final class RuleMapping
    implements Comparable<RuleMapping>
{

    public static final String DEFAULT_MATCH = "default";

    private final String externalMatch;

    private final AutoProxRule rule;

    private final String scriptName;

    private final String spec;

    public RuleMapping( final String scriptName, final String match, final String spec, final AutoProxRule factory )
    {
        this.scriptName = scriptName;
        this.externalMatch = match;
        this.spec = spec;
        this.rule = factory;
    }

    public RuleMapping( final String scriptName, final String spec, final AutoProxRule factory )
    {
        this.scriptName = scriptName;
        this.spec = spec;
        this.rule = factory;
        this.externalMatch = null;
    }

    public RuleMapping( final String match, final String scriptName, final RuleMapping ruleMapping )
    {
        this.scriptName = scriptName;
        this.externalMatch = match;
        this.rule = ruleMapping.getRule();
        this.spec = ruleMapping.getSpecification();
    }

    public RuleDTO toDTO()
    {
        return new RuleDTO( scriptName, spec );
    }

    public String getScriptName()
    {
        return scriptName;
    }

    public String getExternalMatch()
    {
        return externalMatch;
    }

    public AutoProxRule getRule()
    {
        return rule;
    }

    public boolean matchesName( final String name )
    {
        if ( externalMatch != null )
        {
            if ( externalMatch.length() > 2 && externalMatch.charAt( 0 ) == '/'
                && externalMatch.charAt( externalMatch.length() - 1 ) == '/' )
            {
                return name.matches( externalMatch.substring( 1, externalMatch.length() - 1 ) );
            }
            else if ( externalMatch.endsWith( "*" ) )
            {
                if ( externalMatch.length() == 1 )
                {
                    return true;
                }
                else
                {
                    return name.startsWith( externalMatch.substring( 0, externalMatch.length() - 1 ) );
                }
            }
            else if ( DEFAULT_MATCH.equalsIgnoreCase( externalMatch ) )
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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( scriptName == null ) ? 0 : scriptName.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final RuleMapping other = (RuleMapping) obj;
        if ( scriptName == null )
        {
            if ( other.scriptName != null )
            {
                return false;
            }
        }
        else if ( !scriptName.equals( other.scriptName ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo( final RuleMapping other )
    {
        return scriptName.compareTo( other.scriptName );
    }

}
