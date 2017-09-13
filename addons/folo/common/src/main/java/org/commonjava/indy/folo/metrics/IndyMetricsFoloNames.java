/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.folo.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/2/17.
 */
public class IndyMetricsFoloNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.folo";

    private static final String MODULE_FOLORECORDCACHE_PREFIX_NAME = ".foloRecordCache.";

    public static final String METHOD_FOLORECORDCACHE_SEAL =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME + "seal.";

    public static final String METHOD_FOLORECORDCACHE_RECORDARTIFACT =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME + "recordArtifact.";

    public static final String METHOD_FOLORECORDCACHE_DELETE =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME + "delete.";

    public static final String METHOD_FOLORECORDCACHE_HASINPROGRESSRECORD =
                    MODULE_PREFIX_NAME + MODULE_FOLORECORDCACHE_PREFIX_NAME +"hasInProgressRecord.";
}
