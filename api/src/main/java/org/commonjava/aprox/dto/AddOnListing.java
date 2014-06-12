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
package org.commonjava.aprox.dto;

import java.util.Collections;
import java.util.List;

import org.commonjava.aprox.spi.AproxAddOnID;

public class AddOnListing
{

    private final List<AproxAddOnID> items;

    public AddOnListing( final List<AproxAddOnID> addOnNames )
    {
        this.items = addOnNames;
        Collections.sort( items );
    }

    public List<AproxAddOnID> getItems()
    {
        return items;
    }

}
