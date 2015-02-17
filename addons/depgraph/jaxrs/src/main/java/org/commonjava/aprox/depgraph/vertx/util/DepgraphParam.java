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
