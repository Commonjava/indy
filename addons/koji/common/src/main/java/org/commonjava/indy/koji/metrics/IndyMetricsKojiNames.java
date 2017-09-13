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
package org.commonjava.indy.koji.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 5/26/17.
 */
public class IndyMetricsKojiNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.koji";

    private static final String MODULE_MAVENMETADATA_PREFIX_NAME = ".mavenMetadata.";

    private static final String MODULE_CONTENTMANAGER_PREFIX_NAME = ".contenManager.";

    private static final String MODULE_BUILDAUTHORITY_PREFIX_NAME = ".buildAuthority.";

    public static final String METHOD_MAVENMETADATA_GETMETADATA =
                    MODULE_PREFIX_NAME + MODULE_MAVENMETADATA_PREFIX_NAME + "getMetadata.";

    public static final String METHOD_CONTENTMANAGER_EXISTS =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "exists.";

    public static final String METHOD_CONTENTMANAGER_FINDKOJIBUILDAND =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "retrieve.";

    public static final String METHOD_CONTENTMANAGER_RETRIEVE =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "findKojiBuildAnd";

    public static final String METHOD_BUILDAUTHORITY_ISAUTHORIZED =
                    MODULE_PREFIX_NAME + MODULE_BUILDAUTHORITY_PREFIX_NAME + "isAuthorized.";

}
