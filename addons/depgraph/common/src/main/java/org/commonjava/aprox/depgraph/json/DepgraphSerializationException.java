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
package org.commonjava.aprox.depgraph.json;

import org.commonjava.maven.cartographer.CartoException;

public class DepgraphSerializationException
    extends CartoException
{

    private static final long serialVersionUID = 1L;

    public DepgraphSerializationException( final String message, final Object... params )
    {
        super( message, params );
    }

    public DepgraphSerializationException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

}
