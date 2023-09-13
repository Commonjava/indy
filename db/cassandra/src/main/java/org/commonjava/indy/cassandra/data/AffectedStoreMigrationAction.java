/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.data.StoreDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @deprecated This action is using {@link CassandraStoreDataManager} which is deprecated
 */
@Deprecated
@Named( "cassandra-affected-store-data-migration" )
public class AffectedStoreMigrationAction implements MigrationAction
{

    @Inject
    StoreDataManager dataManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public boolean migrate() throws IndyLifecycleException
    {
        if ( dataManager instanceof CassandraStoreDataManager )
        {
            if ( !((CassandraStoreDataManager)dataManager).isAffectedEmpty() )
            {
                logger.info( "Affected store is not empty. Migration already done." );
                return true;
            }

            logger.info( "Init affected stores based on the store data" );

            ( (CassandraStoreDataManager) dataManager ).initAffectedBy();
        }

        return true;
    }

    @Override
    public int getMigrationPriority()
    {
        return 99;
    }

    @Override
    public String getId()
    {
        return "Init affected store table based on the original store data.";
    }
}
