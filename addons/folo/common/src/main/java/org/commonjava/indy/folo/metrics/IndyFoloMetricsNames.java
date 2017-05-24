package org.commonjava.indy.folo.metrics;

/**
 * Created by xiabai on 5/24/17.
 */
public class IndyFoloMetricsNames
{
    public static final String EXCEPTION = "exception";

    public static final String METER = "meter";

    public static final String TIMER = "timer";

    public static class FoloAdminResourceMetricsNames
    {
        private static final String COMMON_NAME = "org.commonjava.indy.folo.admin.resource";

        public static final String METHOD_RECALCULATERECORD = COMMON_NAME + ".recalculateRecord.";

        public static final String METHOD_GETZIPREPOSITORY = COMMON_NAME + ".getZipRepository.";

        public static final String METHOD_GETREPORT = COMMON_NAME + ".getReport.";

        public static final String METHOD_INITRECORD = COMMON_NAME + ".initRecord.";

        public static final String METHOD_SEALRECORD = COMMON_NAME + ".sealRecord.";

        public static final String METHOD_GETRECORD = COMMON_NAME + ".getRecord.";

        public static final String METHOD_CLEARRECORD = COMMON_NAME + ".clearRecord.";

        public static final String METHOD_GETRECORDIDS = COMMON_NAME + ".getRecordIds.";
    }

    public static class FoloContentAccessResourceMetricsNames
    {
        private static final String COMMON_NAME = "org.commonjava.indy.folo.content.access.resource";

        public static final String METHOD_DOCREATE = COMMON_NAME + ".doCreate.";

        public static final String METHOD_DOHEAD = COMMON_NAME + ".doHead.";

        public static final String METHOD_DOGET = COMMON_NAME + ".doGet.";

    }
}
