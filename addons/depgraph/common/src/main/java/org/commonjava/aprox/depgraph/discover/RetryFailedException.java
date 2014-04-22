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
package org.commonjava.aprox.depgraph.discover;

import java.util.List;

import org.commonjava.maven.cartographer.data.CartoDataException;

public class RetryFailedException
    extends CartoDataException
{

    private static final long serialVersionUID = 1L;

    public RetryFailedException( final String message, final List<Throwable> nested, final Object... params )
    {
        super( message, nested, params );
    }

    public RetryFailedException( final String message, final Object... params )
    {
        super( message, params );
    }

    public RetryFailedException( final String message, final Throwable error, final Object... params )
    {
        super( message, error, params );
    }

}
