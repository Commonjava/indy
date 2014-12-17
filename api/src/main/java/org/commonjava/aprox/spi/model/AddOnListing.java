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
package org.commonjava.aprox.spi.model;

import java.util.Collections;
import java.util.List;

/**
 * DTO used to wrap a listing of available add-ons installed in the system. Wrapper embeds these id's in an "items" list, to work around a known
 * JSON security flaw.
 * <br/>
 * See: <a href="http://stackoverflow.com/questions/3503102/what-are-top-level-json-arrays-and-why-are-they-a-security-risk">
 * http://stackoverflow.com/questions/3503102/what-are-top-level-json-arrays-and-why-are-they-a-security-risk
 * </a>
 */
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
