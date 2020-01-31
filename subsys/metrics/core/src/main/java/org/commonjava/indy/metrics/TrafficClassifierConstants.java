package org.commonjava.indy.metrics;

public final class TrafficClassifierConstants
{
    public static final String FN_CONTENT = "content";

    public static final String FN_CONTENT_MAVEN = "content.maven";

    public static final String FN_CONTENT_NPM = "content.npm";

    public static final String FN_CONTENT_GENERIC = "content.generic";

    public static final String FN_METADATA = "metadata";

    public static final String FN_METADATA_MAVEN = "metadata.maven";

    public static final String FN_METADATA_NPM = "metadata.npm";

    public static final String FN_PROMOTION = "promotion";

    public static final String FN_TRACKING_RECORD = "tracking.record";

    public static final String FN_CONTENT_LISTING = "content.listing";

    public static final String FN_REPO_MGMT = "repo.mgmt";

    public static final String FN_MAVEN_UPLOAD = "maven.upload";

    public static final String FN_MAVEN_DOWNLOAD = "maven.download";

    public static final String FN_NPM_UPLOAD = "npm.upload";

    public static final String FN_NPM_DOWNLOAD = "npm.download";

    public static final String[] FUNCTIONS = {
            FN_CONTENT, FN_CONTENT_MAVEN, FN_CONTENT_NPM, FN_CONTENT_GENERIC,
            FN_METADATA, FN_METADATA_MAVEN, FN_METADATA_NPM,
            FN_PROMOTION, FN_TRACKING_RECORD, FN_CONTENT_LISTING, FN_REPO_MGMT,
            FN_MAVEN_UPLOAD, FN_MAVEN_DOWNLOAD, FN_NPM_UPLOAD, FN_NPM_DOWNLOAD
    };

    private TrafficClassifierConstants(){}
}
