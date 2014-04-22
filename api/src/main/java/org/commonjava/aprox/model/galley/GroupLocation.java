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
package org.commonjava.aprox.model.galley;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public class GroupLocation
    extends CacheOnlyLocation
    implements KeyedLocation
{

    public GroupLocation( final String name )
    {
        super( new StoreKey( StoreType.group, name ) );
    }

    @Override
    public String toString()
    {
        return "GroupLocation [" + getKey() + "]";
    }
}
