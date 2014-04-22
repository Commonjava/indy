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
package org.commonjava.aprox.core.change.sl;

import java.util.Date;

import org.commonjava.shelflife.match.ExpirationMatcher;
import org.commonjava.shelflife.model.Expiration;

public class MaxTimeoutMatcher
    implements ExpirationMatcher
{

    private final long maxTimeout;

    public MaxTimeoutMatcher( final long maxTimeout )
    {
        this.maxTimeout = maxTimeout;
    }

    @Override
    public boolean matches( final Expiration expiration )
    {
        final long expires = expiration.getExpires();
        return expires <= maxTimeout;
    }

    @Override
    public String formatQuery()
    {
        return "expires <= " + new Date( maxTimeout );
    }

}
