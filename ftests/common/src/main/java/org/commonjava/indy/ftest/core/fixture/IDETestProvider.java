package org.commonjava.indy.ftest.core.fixture;

import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.flat.data.DataFileStoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFileManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Created by jdcasey on 5/2/16.
 */
@ApplicationScoped
public class IDETestProvider
{
    private StoreDataManager storeDataManager;

    @Inject
    private StoreEventDispatcher dispatcher;

    @Inject
    private IndyConfiguration config;

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private IndyObjectMapper objectMapper;

    @PostConstruct
    public void start()
    {
        storeDataManager = new DataFileStoreDataManager( dataFileManager, objectMapper, dispatcher, config );
    }

    @Produces
    @Default
    public StoreDataManager getStoreDataManager()
    {
        return storeDataManager;
    }
}
