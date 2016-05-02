/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.flat.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.subsys.datafile.DataFile;

import javax.inject.Inject;

/**
 * Migrating data/aprox to data/indy for existing installations that are being upgraded
 */
public class StoreDataAproxMigrationAction
        implements MigrationAction
{
    private static final String APROX_STORE = "aprox";

    @Inject
    private StoreDataManager data;

    @Override
    public boolean migrate()
            throws IndyLifecycleException
    {
        if ( !( this.data instanceof DataFileStoreDataManager ) )
        {
            return true;
        }

        final DataFileStoreDataManager data = (DataFileStoreDataManager) this.data;

        final DataFile destdir = data.getFileManager()
                                     .getDataFile( DataFileStoreDataManager.INDY_STORE );

        final DataFile srcdir = data.getFileManager().getDataFile( APROX_STORE );

        if ( !destdir.exists() && srcdir.exists() )
        {
            srcdir.getDetachedFile().renameTo( destdir.getDetachedFile() );

            return true;
        }

        return false;
    }

    @Override
    public int getMigrationPriority()
    {
        return 99;
    }

    @Override
    public String getId()
    {
        return "store-data-aprox-migration";
    }
}
