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
package org.commonjava.indy.pkg.maven.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 6/1/17.
 */
public class IndyMetricsPkgMavenNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = "org.commonjava.indy.pkgMaven";

    private static final String MODULE_MAVENMETADATAGENERATOR_PREFIX_NAME = ".MavenMetadataGenerator.";

    public static final String METHOD_MAVENMETADATAGENERATOR_GENERATEFILECONTENT =
                    MODULE_PREFIX_NAME + MODULE_MAVENMETADATAGENERATOR_PREFIX_NAME + "generateFileContent.";

    public static final String METHOD_MAVENMETADATAGENERATOR_GENERATEGROUPILECONTENT =
                    MODULE_PREFIX_NAME + MODULE_MAVENMETADATAGENERATOR_PREFIX_NAME + "generateGroupFileContent.";
}
