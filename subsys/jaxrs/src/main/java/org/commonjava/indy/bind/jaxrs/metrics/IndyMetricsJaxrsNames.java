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
package org.commonjava.indy.bind.jaxrs.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/1/17.
 */
public class IndyMetricsJaxrsNames extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.subsys.jaxrs";

    private static final String MODULE_RESOURCEMANAGEMENT_PREFIX_NAME = ".resourceManagement.";

    private static final String MODULE_PROXYRESPONSEWRITER_PREFIX_NAME = ".proxyResponseWriter.";

    public static final String METHOD_RESOURCEMANAGEMENT_DOFILTERE =
                    MODULE_PREFIX_NAME + MODULE_RESOURCEMANAGEMENT_PREFIX_NAME + "doFilter.";

    public static final String METHOD_PROXYRESPONSEWRITER_HANDLEEVENT =
                    MODULE_PREFIX_NAME + MODULE_PROXYRESPONSEWRITER_PREFIX_NAME + "handleEvent.";
}
