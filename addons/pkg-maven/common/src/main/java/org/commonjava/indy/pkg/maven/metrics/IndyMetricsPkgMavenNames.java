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
