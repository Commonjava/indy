package org.commonjava.indy.koji.metrics;

import org.commonjava.indy.IndyMetricsNames;

/**
 * Created by xiabai on 5/26/17.
 */
public class IndyMetricsKojiNames
                extends IndyMetricsNames
{
    private static final String MODULE_PREFIX_NAME = INDY_METRICS_NAME_PREFIX + ".koji";

    private static final String MODULE_MAVENMETADATA_PREFIX_NAME = ".mavenMetadata.";

    private static final String MODULE_CONTENTMANAGER_PREFIX_NAME = ".contenManager.";

    public static final String METHOD_MAVENMETADATA_GETMETADATA =
                    MODULE_PREFIX_NAME + MODULE_MAVENMETADATA_PREFIX_NAME + "getMetadata.";

    public static final String METHOD_CONTENTMANAGER_CREATEREPOCREATOR =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "createRepoCreator.";

    public static final String METHOD_CONTENTMANAGER_EXISTS =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "exists.";

    public static final String METHOD_CONTENTMANAGER_RETRIEVE =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "retrieve.";

    public static final String METHOD_CONTENTMANAGER_GETTRANSFER =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "getTransfer.";

    public static final String METHOD_CONTENTMANAGER_GETTRANSFERWITHARIFACTSTORE =
                    MODULE_PREFIX_NAME + MODULE_CONTENTMANAGER_PREFIX_NAME + "getTransferWithArtifactStore.";



}
