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

import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_EVENT;
import static org.commonjava.aprox.core.change.sl.ExpirationConstants.APROX_FILE_EVENT;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.shelflife.match.PrefixMatcher;

public class StoreMatcher
    extends PrefixMatcher
{

    public StoreMatcher( final StoreKey key )
    {
        super( APROX_EVENT, APROX_FILE_EVENT, key.getType()
                                                 .name(), key.getName() );
    }

    public StoreMatcher()
    {
        super( APROX_EVENT, APROX_FILE_EVENT );
    }

}
