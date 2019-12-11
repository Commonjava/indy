package org.commonjava.indy.db.common;

import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.commonjava.indy.model.core.ArtifactStore;

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
        ArtifactStoreQuery<ArtifactStore> query = dataManager.query();
        return new MeasuringStoreQuery( query, metricsManager );
    }
}
