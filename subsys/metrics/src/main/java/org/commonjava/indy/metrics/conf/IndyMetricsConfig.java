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
public class IndyMetricsConfig
                implements IndyConfigInfo
{
    public static final String SECTION = "metrics";

    public final static String INDY_METRICS_REPORTER = "reporter";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_HOSTNAME = "graphite.hostname";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_PORT = "graphite.port";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_PREFIX = "graphite.reporter.prefix";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_SIMPLE_PERIOD = "graphite.reporter.simple.period";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_JVM_PERIOD = "graphite.reporter.jvm.period";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER_HEALTHCHECK_PERIOD =
                    "graphite.reporter.healthcheck.period";

    public final static int INDY_METRICS_REPORTER_GRPHITEREPORTER_DEFAULT_PERIOD = 30;

    public final static String INDY_METRICS_ISENABLED = "enabled";

    public final static String INDY_METRICS_REPORTER_ISENABLED = "reporter.enabled";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_API_HOST_URL = "zabbix.api.url";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_HOST_PORT = "zabbix.sender.port";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_HOST = "zabbix.sender.host";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_USER = "zabbix.user";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_PWD = "zabbix.pwd";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_LOCAL_HOSTNAME = "zabbix.indy.host";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_PREFIX = "zabbix.reporter.prefix";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_SIMPLE_PERIOD = "zabbix.reporter.simple.period";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_JVM_PERIOD = "zabbix.reporter.jvm.period";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER_HEALTHCHECK_PERIOD =
                    "zabbix.reporter.healthcheck.period";

    public final static String INDY_METRICS_REPORTER_ELKREPORTER_PREFIX = "elk.reporter.prefix";

    public final static String INDY_METRICS_REPORTER_ELKREPORTER_SIMPLE_PERIOD = "elk.reporter.simple.period";

    public final static String INDY_METRICS_REPORTER_ELKREPORTER_JVM_PERIOD = "elk.reporter.jvm.period";

    public final static String INDY_METRICS_REPORTER_ELKREPORTER_HEALTHCHECK_PERIOD = "elk.reporter.healthcheck.period";

    public final static String INDY_METRICS_REPORTER_ELKREPORTER_INDEX = "elk.reporter.index";

    public final static String INDY_METRICS_REPORTER_ELKREPORTER_HOSTS = "elk.reporter.hosts";

    private String elkPrefix;

    private int elkSimplePriod;

    private int elkJVMPriod;

    private int elkHealthCheckPriod;

    private String elkHosts;

    private String elkIndex;

    private String zabbixPrefix;

    private int zabbixSimplePriod;

    private int zabbixJVMPriod;

    private int zabbixHealthcheckPeriod;

    private String zabbixApiHostUrl;

    private String zabbixHost;

    private int zabbixPort;

    private String zabbixUser;

    private String zabbixPwd;

    private String zabbixLocalHostName;

    private String reporter;

    private String grphiterHostName;

    private int grphiterPort;

    private String grphiterPrefix;

    private int grphiterSimplePriod;

    private int grphiterJVMPriod;

    private int grphiterHealthcheckPeriod;

    private boolean metricsEnabled;

    private boolean reporterEnabled;


    public String getElkPrefix()
    {
        return elkPrefix;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ELKREPORTER_PREFIX )
    public void setElkPrefix( String elkPrefix )
    {
        this.elkPrefix = elkPrefix;
    }

    public int getElkSimplePriod()
    {
        return elkSimplePriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ELKREPORTER_SIMPLE_PERIOD )
    public void setElkSimplePriod( int elkSimplePriod )
    {
        this.elkSimplePriod = elkSimplePriod;
    }

    public int getElkJVMPriod()
    {
        return elkJVMPriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ELKREPORTER_JVM_PERIOD )
    public void setElkJVMPriod( int elkJVMPriod )
    {
        this.elkJVMPriod = elkJVMPriod;
    }

    public int getElkHealthCheckPriod()
    {
        return elkHealthCheckPriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ELKREPORTER_HEALTHCHECK_PERIOD )
    public void setElkHealthCheckPriod( int elkHealthCheckPriod )
    {
        this.elkHealthCheckPriod = elkHealthCheckPriod;
    }

    public String getElkHosts()
    {
        return elkHosts;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ELKREPORTER_HOSTS )
    public void setElkHosts( String elkHosts )
    {
        this.elkHosts = elkHosts;
    }

    public String getElkIndex()
    {
        return elkIndex;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ELKREPORTER_INDEX )
    public void setElkIndex( String elkIndex )
    {
        this.elkIndex = elkIndex;
    }

    public String getZabbixPrefix()
    {
        return zabbixPrefix;
    }
    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_PREFIX )
    public void setZabbixPrefix( String zabbixPrefix )
    {
        this.zabbixPrefix = zabbixPrefix;
    }

    public int getZabbixSimplePriod()
    {
        return zabbixSimplePriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_SIMPLE_PERIOD )
    public void setZabbixSimplePriod( int zabbixSimplePriod )
    {
        this.zabbixSimplePriod = zabbixSimplePriod;
    }

    public int getZabbixJVMPriod()
    {
        return zabbixJVMPriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_JVM_PERIOD )
    public void setZabbixJVMPriod( int zabbixJVMPriod )
    {
        this.zabbixJVMPriod = zabbixJVMPriod;
    }

    public int getZabbixHealthcheckPeriod()
    {
        return zabbixHealthcheckPeriod;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_HEALTHCHECK_PERIOD )
    public void setZabbixHealthcheckPeriod( int zabbixHealthcheckPeriod )
    {
        this.zabbixHealthcheckPeriod = zabbixHealthcheckPeriod;
    }

    public String getZabbixApiHostUrl()
    {
        return zabbixApiHostUrl;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_API_HOST_URL )
    public void setZabbixApiHostUrl( String zabbixApiHostUrl )
    {
        this.zabbixApiHostUrl = zabbixApiHostUrl;
    }

    public String getZabbixHost()
    {
        return zabbixHost;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_HOST )
    public void setZabbixHost( String zabbixHost )
    {
        this.zabbixHost = zabbixHost;
    }

    public int getZabbixPort()
    {
        return zabbixPort;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_HOST_PORT )
    public void setZabbixPort( int zabbixPort )
    {
        this.zabbixPort = zabbixPort;
    }

    public String getZabbixUser()
    {
        return zabbixUser;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_USER )
    public void setZabbixUser( String zabbixUser )
    {
        this.zabbixUser = zabbixUser;
    }

    public String getZabbixPwd()
    {
        return zabbixPwd;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_PWD )
    public void setZabbixPwd( String zabbixPwd )
    {
        this.zabbixPwd = zabbixPwd;
    }

    public String getZabbixLocalHostName()
    {
        return zabbixLocalHostName;
    }

    @ConfigName( IndyMetricsConfig.INDY_METRICS_REPORTER_ZABBIXREPORTER_LOCAL_HOSTNAME )
    public void setZabbixLocalHostName( String zabbixLocalHostName )
    {
        this.zabbixLocalHostName = zabbixLocalHostName;
    }

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
