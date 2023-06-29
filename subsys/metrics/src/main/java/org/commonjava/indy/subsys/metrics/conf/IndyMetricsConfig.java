/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.metrics.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.o11yphant.metrics.conf.ConsoleConfig;
import org.commonjava.o11yphant.metrics.conf.ELKConfig;
import org.commonjava.o11yphant.metrics.conf.GraphiteConfig;
import org.commonjava.o11yphant.metrics.conf.MetricsConfig;
import org.commonjava.o11yphant.metrics.conf.PrometheusConfig;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Arrays;

import static org.commonjava.indy.subsys.metrics.conf.IndyMetricsConfig.SECTION;

@SectionName( SECTION )
@ApplicationScoped
public class IndyMetricsConfig
                implements IndyConfigInfo, MetricsConfig
{
    public static final String SECTION = "metrics";

    public final static String NODE_PREFIX = "node.prefix";

    private final static String REPORTER = "reporter";

    private final static String REPORTER_CONSOLE_PERIOD = "console.reporter.period";

    private final static String GRAPHITE_HOSTNAME = "graphite.hostname";

    private final static String GRAPHITE_PORT = "graphite.port";

    private final static String GRAPHITE_REPORTER_PREFIX = "graphite.reporter.prefix";

    private final static String GRAPHITE_REPORTER_SIMPLE_PERIOD = "graphite.reporter.simple.period";

    private final static String GRAPHITE_REPORTER_JVM_PERIOD = "graphite.reporter.jvm.period";

    private final static String GRAPHITE_REPORTER_HEALTHCHECK_PERIOD = "graphite.reporter.healthcheck.period";

    private final static String ENABLED = "enabled";

    private static final String METER_RATIO = "meter.ratio";

    private final static String ISPN_ENABLED = "ispn.enabled";

    private final static String ISPN_GAUGES = "ispn.gauges";

    private final static String ELK_REPORTER_PREFIX = "elk.reporter.prefix";

    private final static String ELK_REPORTER_SIMPLE_PERIOD = "elk.reporter.simple.period";

    private final static String ELK_REPORTER_JVM_PERIOD = "elk.reporter.jvm.period";

    private final static String ELK_REPORTER_HEALTHCHECK_PERIOD = "elk.reporter.healthcheck.period";

    private final static String ELK_REPORTER_INDEX = "elk.reporter.index";

    private final static String ELK_REPORTER_HOSTS = "elk.reporter.hosts";

    private static final String MEASURE_TRANSPORT = "measure.transport";

    private static final String MEASURE_TRANSPORT_REPOS = "measure.transport.repos";

    private final static String KOJI_ENABLED = "koji.enabled";

    private final static String PATH_DB_ENABLED = "pathdb.enabled";

    private final static String PATH_DB_OPERATIONS = "pathdb.operations";

    private final static String PROMETHEUS_EXPRESSED_METRICS = "prometheus.expressed.metrics";

    private static final String PROMETHEUS_NODE_LABEL = "prometheus.node.label";

    private static final int DEFAULT_METER_RATIO = 1;

    private boolean ispnMetricsEnabled;

    private boolean pathDBMetricsEnabled = true; // default

    private String pathDBMetricsOperations;

    private String ispnGauges;

    private boolean measureTransport;

    private String measureTransportRepos;

    private int consolePeriod = 30; // default

    private String elkPrefix;

    private int elkSimplePeriod;

    private int elkJVMPeriod;

    private int elkHealthCheckPeriod;

    private String elkHosts;

    private String elkIndex;

    private String reporter;

    private String graphiteHostName;

    private int graphitePort;

    private String graphitePrefix;

    private int graphiteSimplePeriod;

    private int graphiteJVMPeriod;

    private int graphiteHealthcheckPeriod;

    private boolean metricsEnabled;

    private boolean kojiMetricEnabled;

    private String nodePrefix;

    private Integer meterRatio;

    private String prometheusExpressedMetrics;

    private String prometheusNodeLabel;

    public boolean isMeasureTransport()
    {
        return measureTransport;
    }

    @ConfigName( NODE_PREFIX )
    public void setNodePrefix( String nodePrefix )
    {
        this.nodePrefix = nodePrefix;
    }

    public String getNodePrefix()
    {
        return nodePrefix;
    }

    @ConfigName( MEASURE_TRANSPORT )
    public void setMeasureTransport( boolean measureTransport )
    {
        this.measureTransport = measureTransport;
    }

    public String getMeasureTransportRepos()
    {
        return measureTransportRepos;
    }

    @ConfigName( MEASURE_TRANSPORT_REPOS )
    public void setMeasureTransportRepos( String measureTransportRepos )
    {
        this.measureTransportRepos = measureTransportRepos;
    }

    @ConfigName( REPORTER_CONSOLE_PERIOD )
    public void setConsolePeriod( int consolePeriod )
    {
        this.consolePeriod = consolePeriod;
    }

    @ConfigName( ELK_REPORTER_PREFIX )
    public void setElkPrefix( String elkPrefix )
    {
        this.elkPrefix = elkPrefix;
    }

    @ConfigName( ELK_REPORTER_SIMPLE_PERIOD )
    public void setElkSimplePeriod( int elkSimplePeriod )
    {
        this.elkSimplePeriod = elkSimplePeriod;
    }

    @ConfigName( ELK_REPORTER_JVM_PERIOD )
    public void setElkJVMPeriod( int elkJVMPeriod )
    {
        this.elkJVMPeriod = elkJVMPeriod;
    }

    @ConfigName( ELK_REPORTER_HEALTHCHECK_PERIOD )
    public void setElkHealthCheckPeriod( int elkHealthCheckPeriod )
    {
        this.elkHealthCheckPeriod = elkHealthCheckPeriod;
    }

    @ConfigName( ELK_REPORTER_HOSTS )
    public void setElkHosts( String elkHosts )
    {
        this.elkHosts = elkHosts;
    }

    @ConfigName( ELK_REPORTER_INDEX )
    public void setElkIndex( String elkIndex )
    {
        this.elkIndex = elkIndex;
    }

    public boolean isEnabled()
    {
        return metricsEnabled;
    }

    @ConfigName( ENABLED )
    public void setEnabled( boolean metricsEnabled )
    {
        this.metricsEnabled = metricsEnabled;
    }

    @ConfigName( METER_RATIO )
    public void setMeterRatio( int meterRatio )
    {
        this.meterRatio = meterRatio;
    }

    @Override
    public int getMeterRatio()
    {
        return meterRatio == null ? DEFAULT_METER_RATIO : meterRatio;
    }

    @Override
    public String getReporter()
    {
        return reporter;
    }

    @ConfigName( REPORTER )
    public void setReporter( String reporter )
    {
        this.reporter = reporter;
    }

    @ConfigName( GRAPHITE_HOSTNAME )
    public void setGraphiteHostName( String graphiteHostName )
    {
        this.graphiteHostName = graphiteHostName;
    }

    @ConfigName( GRAPHITE_PORT )
    public void setGraphitePort( int graphitePort )
    {
        this.graphitePort = graphitePort;
    }

    @ConfigName( GRAPHITE_REPORTER_PREFIX )
    public void setGraphitePrefix( String graphitePrefix )
    {
        this.graphitePrefix = graphitePrefix;
    }

    @ConfigName( GRAPHITE_REPORTER_SIMPLE_PERIOD )
    public void setGraphiteSimplePeriod( int graphiteSimplePeriod )
    {
        this.graphiteSimplePeriod = graphiteSimplePeriod;
    }

    @ConfigName( GRAPHITE_REPORTER_JVM_PERIOD )
    public void setGraphiteJVMPeriod( int graphiteJVMPeriod )
    {
        this.graphiteJVMPeriod = graphiteJVMPeriod;
    }

    @ConfigName( GRAPHITE_REPORTER_HEALTHCHECK_PERIOD )
    public void setGraphiteHealthcheckPeriod( int graphiteHealthcheckPeriod )
    {
        this.graphiteHealthcheckPeriod = graphiteHealthcheckPeriod;
    }

    @ConfigName( KOJI_ENABLED )
    public void setKojiMetricEnabled( boolean kojiMetricEnabled )
    {
        this.kojiMetricEnabled = kojiMetricEnabled;
    }

    public boolean isKojiMetricEnabled()
    {
        return kojiMetricEnabled;
    }

    public boolean isPathDBMetricsEnabled()
    {
        return pathDBMetricsEnabled;
    }

    @ConfigName( PATH_DB_ENABLED )
    public void setPathDBMetricsEnabled( boolean pathDBMetricsEnabled )
    {
        this.pathDBMetricsEnabled = pathDBMetricsEnabled;
    }

    public String getPathDBMetricsOperations()
    {
        return pathDBMetricsOperations;
    }

    @ConfigName( PATH_DB_OPERATIONS )
    public void setPathDBMetricsOperations( String pathDBMetricsOperations )
    {
        this.pathDBMetricsOperations = pathDBMetricsOperations;
    }

    @ConfigName( ISPN_ENABLED )
    public void setIspnMetricsEnabled( boolean ispnMetricsEnabled )
    {
        this.ispnMetricsEnabled = ispnMetricsEnabled;
    }

    public boolean isIspnMetricsEnabled()
    {
        return ispnMetricsEnabled;
    }

    @ConfigName( ISPN_GAUGES )
    public void setIspnGauges( String ispnGauges )
    {
        this.ispnGauges = ispnGauges;
    }

    public String getIspnGauges()
    {
        return ispnGauges;
    }

    @Override
    public ConsoleConfig getConsoleConfig()
    {
        ConsoleConfig ret = new ConsoleConfig();
        ret.setConsolePeriodInSeconds( consolePeriod );
        return ret;
    }

    @Override
    public GraphiteConfig getGraphiteConfig()
    {
        GraphiteConfig ret = new GraphiteConfig();
        ret.setGraphiteHostName( graphiteHostName );
        ret.setGraphitePort( graphitePort );
        ret.setGraphitePeriodInSeconds( graphiteSimplePeriod );
        ret.setGraphitePrefix( graphitePrefix );
        ret.setGraphiteJVMPeriodInSeconds( graphiteJVMPeriod );
        ret.setGraphiteHealthcheckPeriodInSeconds( graphiteHealthcheckPeriod );
        return ret;
    }

    @Override
    public ELKConfig getELKConfig()
    {
        ELKConfig ret = new ELKConfig();
        ret.setElkHosts( elkHosts );
        ret.setElkIndex( elkIndex );
        ret.setElkPeriodInSeconds( elkSimplePeriod );
        ret.setElkJVMPeriodInSeconds( elkJVMPeriod );
        ret.setElkPrefix( elkPrefix );
        ret.setElkHealthCheckPeriodInSeconds( elkHealthCheckPeriod );
        return ret;
    }

    public PrometheusConfig getPrometheusConfig()
    {
        PrometheusConfig ret = new PrometheusConfig();
        ret.setNodeLabel( prometheusNodeLabel );
        if ( prometheusExpressedMetrics != null )
        {
            ret.setExpressedMetrics( Arrays.asList( prometheusExpressedMetrics.split( "," ) ) );
        }

        return ret;
    }

    public String getDefaultConfigFileName()
    {
        return "conf.d/metrics.conf";
    }

    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-metrics.conf" );
    }

    public String getPrometheusExpressedMetrics()
    {
        return prometheusExpressedMetrics;
    }

    @ConfigName( PROMETHEUS_EXPRESSED_METRICS )
    public void setPrometheusExpressedMetrics( String prometheusExpressedMetrics )
    {
        this.prometheusExpressedMetrics = prometheusExpressedMetrics;
    }

    public String getPrometheusNodeLabel()
    {
        return prometheusNodeLabel;
    }

    @ConfigName( PROMETHEUS_NODE_LABEL )
    public void setPrometheusNodeLabel( String prometheusNodeLabel )
    {
        this.prometheusNodeLabel = prometheusNodeLabel;
    }
}
