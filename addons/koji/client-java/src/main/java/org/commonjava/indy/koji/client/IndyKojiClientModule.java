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
package org.commonjava.indy.koji.client;

import org.apache.http.HttpStatus;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.koji.model.KojiRepairRequest;
import org.commonjava.indy.koji.model.KojiRepairResult;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

import static org.commonjava.indy.koji.model.IndyKojiConstants.REPAIR_KOJI_VOL;

public class IndyKojiClientModule
                extends IndyClientModule
{

    public KojiRepairResult repairVol( final String packageType, final StoreType storeType, final String storeName,
                                            final boolean dryRun ) throws IndyClientException
    {
        KojiRepairRequest req = new KojiRepairRequest( new StoreKey( packageType, storeType, storeName ), dryRun );

        KojiRepairResult result =
                        http.postWithResponse( REPAIR_KOJI_VOL, req, KojiRepairResult.class, HttpStatus.SC_OK );

        return result;
    }

}
