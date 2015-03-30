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

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.maven.galley.model.Location;

/**
 * {@link Location} that references a particular {@link StoreKey}, bridging Galley transfer management with the store definitions of AProx.
 */
public interface KeyedLocation
    extends Location
{

    StoreKey getKey();

}
