package org.commonjava.indy.metrics.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Singleton;
import java.io.InputStream;

/**
 * Created by xiabai on 3/17/17.
 */
@SectionName( IndyMetricsConfig.SECTION )
@ApplicationScoped
//@Singleton
public class IndyMetricsConfig
                implements IndyConfigInfo
{
    public static final String SECTION = "metrics";

    public final static String INDY_METRICS_REPORTER = "reporter";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER = "graphite.reporter";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_HOSTNAME = "graphite.reporter.hostname";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_PORT = "graphite.reporter.port";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_PREFIX = "graphite.reporter.prefix";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_SIMPLE_PERIOD = "graphite.reporter.simple.period";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_JVM_PERIOD = "graphite.reporter.jvm.period";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_HEALTHCHECK_PERIOD =
                    "graphite.reporter.healthcheck.period";

    public final static int INDY_METRICS_REPORTER_GRPHITEREPORTER_DEFAULT_PERIOD = 30;

    public final static String INDY_METRICS_ISENABLED = "enabled";
    public final static String INDY_METRICS_REPORTER_ISENABLED = "reporter.enabled";

    private String reporter;

    private String grphiterHostName;

    private int grphiterPort;

    private String grphiterPrefix;

    private int grphiterSimplePriod;

    private int grphiterJVMPriod;

    private int grphiterHealthcheckPeriod;

    private boolean metricsEnabled;
    private boolean reporterEnabled;

    public boolean isReporterEnabled()
    {
        return reporterEnabled;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ISENABLED )
    public void setReporterEnabled( boolean reporterEnabled )
    {
        this.reporterEnabled = reporterEnabled;
    }

    public boolean isMetricsEnabled()
    {
        return metricsEnabled;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_ISENABLED )
    public void setMetricsEnabled( boolean metricsEnabled )
    {
        this.metricsEnabled = metricsEnabled;
    }

    public String getReporter()
    {
        return reporter;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER )
    public void setReporter( String reporter )
    {
        this.reporter = reporter;
    }

    public String getGrphiterHostName()
    {
        return grphiterHostName;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_HOSTNAME )
    public void setGrphiterHostName( String grphiterHostName )
    {
        this.grphiterHostName = grphiterHostName;
    }

    public int getGrphiterPort()
    {
        return grphiterPort;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_PORT )
    public void setGrphiterPort( int grphiterPort )
    {
        this.grphiterPort = grphiterPort;
    }

    public String getGrphiterPrefix()
    {
        return grphiterPrefix;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_PREFIX )
    public void setGrphiterPrefix( String grphiterPrefix )
    {
        this.grphiterPrefix = grphiterPrefix;
    }

    public int getGrphiterSimplePriod()
    {
        return grphiterSimplePriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_SIMPLE_PERIOD )
    public void setGrphiterSimplePriod( int grphiterSimplePriod )
    {
        this.grphiterSimplePriod = grphiterSimplePriod;
    }

    public int getGrphiterJVMPriod()
    {
        return grphiterJVMPriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_JVM_PERIOD )
    public void setGrphiterJVMPriod( int grphiterJVMPriod )
    {
        this.grphiterJVMPriod = grphiterJVMPriod;
    }

    public int getGrphiterHealthcheckPeriod()
    {
        return grphiterHealthcheckPeriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_HEALTHCHECK_PERIOD )
    public void setGrphiterHealthcheckPeriod( int grphiterHealthcheckPeriod )
    {
        this.grphiterHealthcheckPeriod = grphiterHealthcheckPeriod;
    }

    public String getDefaultConfigFileName()
    {
        return "conf.d/metrics.conf";
    }

    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-metrics.conf" );
    }

}
