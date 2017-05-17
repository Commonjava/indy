package org.commonjava.indy;

/**
 * Constant names used for metrics timers, meters, and exception meters. These are for use in Indy's core operations.
 * These should be used in {@link org.commonjava.indy.measure.annotation.MetricNamed} annotations.
 * <br/>
 * <b>NOTE:</b> Add-ons may define their own constant classes for the same purpose, scoped to the add-on's operations.
 * These should be place in the add-on's *-common module.
 * <br/>
 * Created by jdcasey on 5/9/17.
 */
public class IndyMetricsNames
{
    public static final String EXCEPTION_CONTENT_RETRIEVAL = "org.commonjava.indy.content.get.exception";

    public static final String METER_CONTENT_RETRIEVAL = "org.commonjava.indy.content.get.meter";
    public static final String METER_REST_REQUEST = "org.commonjava.indy.rest.inbound.meter";

    public static final String TIMER_CONTENT_RETRIEVAL = "org.commonjava.indy.content.get.timer";
    public static final String TIMER_REST_REQUEST = "org.commonjava.indy.rest.inbound.timer";
}
