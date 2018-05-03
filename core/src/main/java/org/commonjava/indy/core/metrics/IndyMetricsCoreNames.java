/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/1/17.
 */
public class IndyMetricsCoreNames extends IndyMetricsNames
{
    private static final String DOWNLOAD_MANAGER_PREFIX = "org.commonjava.indy.core.download-manager.";

    public static final String DOWNLOADMGR_LIST = DOWNLOAD_MANAGER_PREFIX + "list";

    public static final String DOWNLOADMGR_LIST_RECURSIVE = "list-recursive";

    public static final String DOWNLOADMGR_RETRIEVE = DOWNLOAD_MANAGER_PREFIX + "retrieve";

    public static final String DOWNLOADMGR_RETRIEVE_ALL = DOWNLOAD_MANAGER_PREFIX + "retrieve-all";

    public static final String DOWNLOADMGR_RETRIEVE_FIRST = DOWNLOAD_MANAGER_PREFIX + "retrieve-first";

    public static final String DOWNLOADMGR_STORE = DOWNLOAD_MANAGER_PREFIX + "store";

    public static final String DOWNLOADMGR_EXISTS = DOWNLOAD_MANAGER_PREFIX + "exists";

    public static final String DOWNLOADMGR_GET_TRANSFER = DOWNLOAD_MANAGER_PREFIX + "get-transfer";

    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.core";

    private static final String MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME = ".defaultContentManager.";

    private static final String MODULE_STORECONTENTLISTENER_PREFIX_NAME = ".storeContentListener.";

    private static final String MODULE_STOREENABLEMENT_PREFIX_NAME = ".StoreEnablement.";

    public static final String METHOD_STOREENABLEMENT_ONDESABLETIMEOUT =
                    MODULE_PREFIX_NAME + MODULE_STOREENABLEMENT_PREFIX_NAME    + "onDisableTimeout.";

    public static final String METHOD_STOREENABLEMENT_ONDSTOREENABLEMENTCHANGE =
                    MODULE_PREFIX_NAME + MODULE_STOREENABLEMENT_PREFIX_NAME    + "onStoreEnablementChange.";

    public static final String METHOD_STOREENABLEMENT_ONDSTOREEERROR =
                    MODULE_PREFIX_NAME + MODULE_STOREENABLEMENT_PREFIX_NAME    + "onStoreError.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_RETRIEVE =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "retrieve.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_DELETE =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "delete.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_STORE =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "store.";

    public static final String METHOD_DEFAULTCONTENTMANAGER_LIST =
                    MODULE_PREFIX_NAME + MODULE_DEFAULTCONTENTMANAGER_PREFIX_NAME    + "list.";


    public static final String METHOD_STORECONTENTLISTENER_ONSTOREUPDATE =
                    MODULE_PREFIX_NAME + MODULE_STORECONTENTLISTENER_PREFIX_NAME    + "onStoreUpdate.";

    public static final String METHOD_STORECONTENTLISTENER_ONSTOREDISABLE =
                    MODULE_PREFIX_NAME + MODULE_STORECONTENTLISTENER_PREFIX_NAME    + "onStoreDisable.";

    public static final String METHOD_STORECONTENTLISTENER_ONSTOREDELETION =
                    MODULE_PREFIX_NAME + MODULE_STORECONTENTLISTENER_PREFIX_NAME    + "onStoreDeletion.";
}
