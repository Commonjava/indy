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
package org.commonjava.aprox.depgraph.vertx.util;

public enum DepgraphParam
{

    p_from,
    p_wsid,
    p_source,
    p_profile,
    p_key,
    p_gav,
    p_groupId,
    p_artifactId,
    p_version,
    p_newVersion,
    q_for( "q:for" ),
    q_groupId( "q:g" ),
    q_artifactId( "q:a" ),
    q_scope( "q:s" ),
    q_scopes( "q:scopes" ),
    q_recurse( "q:recurse" ),
    q_wsid( "q:wsid" );

    private String k;

    private DepgraphParam()
    {
    }

    private DepgraphParam( final String key )
    {
        this.k = key;
    }

    public String key()
    {
        if ( k == null )
        {
            String nom = name();
            final int idx = nom.indexOf( '_' );
            if ( idx > 0 )
            {
                nom = nom.substring( idx + 1 );
            }

            return nom;
        }

        return k;
    }

}
