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
package org.commonjava.indy.promote.client;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;

import static org.commonjava.indy.client.core.util.UrlUtils.buildUrl;
import static org.commonjava.indy.promote.client.IndyPromoteClientModule.PROMOTE_BASEPATH;

public class IndyPromoteAdminClientModule
        extends IndyClientModule
{
    public static final String PROMOTE_ADMIN_BASEPATH = PROMOTE_BASEPATH + "/admin";

    public static final String TRACKING = PROMOTE_ADMIN_BASEPATH + "/tracking";

    public void deleteTrackingRecords( final String trackingId )
            throws IndyClientException
    {
        http.delete( buildUrl( TRACKING, trackingId ) );
    }
}
