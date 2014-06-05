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

public class FactoryMapping
{

    public static final String DEFAULT_MATCH = "default";

    private final String match;

    private final AutoProxFactory factory;

    private final String scriptName;

    public FactoryMapping( final String scriptName, final String match, final AutoProxFactory factory )
    {
        this.scriptName = scriptName;
        this.match = match;
        this.factory = factory;
    }

    public String getScriptName()
    {
        return scriptName;
    }

    public String getMatch()
    {
        return match;
    }

    public AutoProxFactory getFactory()
    {
        return factory;
    }

    public boolean matchesName( final String name )
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

        return false;
    }

}
