/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
public class TestProvider
{
/*    private StoreDataManager storeDataManager;

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
        storeDataManager = new DataFileStoreDataManager( dataFileManager, objectMapper, dispatcher );
    }

//    @Produces
//    @Default
    public StoreDataManager getStoreDataManager()
    {
        return storeDataManager;
    }*/
}
