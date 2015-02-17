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
package org.commonjava.aprox.util;

public enum ApplicationHeader
{

    content_type( "Content-Type" ),
    location( "Location" ),
    uri( "URI" ),
    content_length( "Content-Length" ),
    last_modified( "Last-Modified" ),
    deprecated( "Deprecated-Use-Alt" ),
    accept( "Accept" );

    private final String key;

    private ApplicationHeader( final String key )
    {
        this.key = key;
    }

    public String key()
    {
        return key;
    }

}
