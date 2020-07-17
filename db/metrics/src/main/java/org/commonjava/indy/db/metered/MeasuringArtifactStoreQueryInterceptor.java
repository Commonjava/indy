/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.db.metered;

import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.subsys.metrics.IndyMetricsManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

@Decorator
public abstract class MeasuringArtifactStoreQueryInterceptor
    implements StoreDataManager
{
    @Inject
    @Delegate
    private StoreDataManager dataManager;

    @Inject
    private IndyMetricsManager metricsManager;

    @Override
    public ArtifactStoreQuery<ArtifactStore> query()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "WRAP in metered ArtifactStoreQuery" );
        ArtifactStoreQuery<ArtifactStore> query = dataManager.query();
        return new MeasuringStoreQuery( query, metricsManager );
    }
}
