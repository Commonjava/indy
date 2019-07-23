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
package org.commonjava.indy.ftest.metrics.client;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;

/**
 * Created by xiabai on 5/8/17.
 */
public class ZabbixCacheStorageTestClientModule  extends IndyClientModule
{
    private static String PUT_PATH = "/ftest/metrics/storage/put";
    private static String GET_HOSTGROUP_PATH = "/ftest/metrics/storage/putHostGroup";
    private static String GET_HOST_PATH = "/ftest/metrics/storage/putHost";
    private static String GET_ITEM_PATH = "/ftest/metrics/storage/putItem";

    public String putCache() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", PUT_PATH ), String.class );
    }

    public String getHostGroupCache() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", GET_HOSTGROUP_PATH ), String.class );
    }

    public String getHostCache() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", GET_HOST_PATH ), String.class );
    }

    public String getItemCache() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", GET_ITEM_PATH ), String.class );
    }

}
