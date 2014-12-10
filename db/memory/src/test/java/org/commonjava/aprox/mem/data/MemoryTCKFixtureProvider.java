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
package org.commonjava.aprox.mem.data;

import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.core.data.testutil.StoreEventDispatcherStub;
import org.commonjava.aprox.data.StoreDataManager;

public class MemoryTCKFixtureProvider
    implements TCKFixtureProvider
{

    private final MemoryStoreDataManager dataManager = new MemoryStoreDataManager( new StoreEventDispatcherStub() );

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

}
