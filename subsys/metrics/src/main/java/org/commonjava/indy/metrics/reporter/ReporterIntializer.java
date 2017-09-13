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
package org.commonjava.indy.metrics.reporter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.commonjava.indy.metrics.conf.annotation.IndyMetricsNamed;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.metrics.zabbix.cache.ZabbixCacheStorage;
import org.commonjava.indy.metrics.zabbix.reporter.IndyZabbixReporter;
import org.commonjava.indy.metrics.zabbix.sender.IndyZabbixSender;
import org.commonjava.indy.subsys.http.IndyHttpProvider;
import org.elasticsearch.metrics.ElasticsearchReporter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiabai on 3/3/17.
 */
@ApplicationScoped
public class ReporterIntializer
{
    private final static String FILTER_SIMPLE = "org.commonjava.indy";

    private final static String FILTER_JVM = "jvm";

    private final static String FILTER_HEALTHCHECK = "healthcheck";

    public final static String INDY_METRICS_REPORTER_GRPHITEREPORTER = "graphite";

    public final static String INDY_METRICS_REPORTER_CONSOLEREPORTER = "console";

    public final static String INDY_METRICS_REPORTER_ZABBIXREPORTER = "zabbix";

    public final static String INDY_METRICS_REPORTER_ELKEPORTER = "elasticsearch";

    @Inject
    IndyHttpProvider indyHttpProvider;

    @Inject
    ZabbixCacheStorage cache;

    @Inject
    @IndyMetricsNamed
    IndyMetricsConfig config;

    public void initReporter( MetricRegistry metrics ) throws Exception
    {

        if ( !config.isReporterEnabled() )
        {
            initConsoleReporter( metrics, config );
            return;
        }
        if ( this.isExistReporter( INDY_METRICS_REPORTER_GRPHITEREPORTER ) )
        {
            initGraphiteReporterForSimpleMetric( metrics, config );
            initGraphiteReporterForJVMMetric( metrics, config );
            initGraphiteReporterForHealthCheckMetric( metrics, config );
        }

        if ( this.isExistReporter( INDY_METRICS_REPORTER_ZABBIXREPORTER ) )
        {
            this.initZabbixReporterForHealthCheckMetric( metrics, config );
            this.initZabbixReporterForJVMMetric( metrics, config );
            this.initZabbixReporterForSimpleMetric( metrics, config );
        }

        if ( this.isExistReporter( INDY_METRICS_REPORTER_CONSOLEREPORTER ) )
        {
            initConsoleReporter( metrics, config );
        }

        if ( this.isExistReporter( INDY_METRICS_REPORTER_ELKEPORTER ) )
        {
            initELKReporterForSimpleMetric( metrics, config );
            initELKReporterForJVMMetric( metrics, config );
            initELKReporterForHealthCheckMetric( metrics, config );
        }
    }

    private void initELKReporterForSimpleMetric( MetricRegistry metrics, IndyMetricsConfig config ) throws IOException
    {
        ElasticsearchReporter reporter = ElasticsearchReporter.forRegistry( metrics )
                                                              .hosts( config.getElkHosts().split( ";" ) )
                                                              .index( config.getElkIndex() )
                                                              .indexDateFormat( "YYYY-MM-dd" )
                                                              .filter( ( name, metric ) ->
                                                                       {
                                                                           if ( name.contains( FILTER_SIMPLE ) )
                                                                           {
                                                                               return true;
                                                                           }
                                                                           return false;
                                                                       } )
                                                              .build();

        reporter.start( config.getElkSimplePriod(), TimeUnit.SECONDS );
    }

    private void initELKReporterForJVMMetric( MetricRegistry metrics, IndyMetricsConfig config ) throws IOException
    {
        ElasticsearchReporter reporter = ElasticsearchReporter.forRegistry( metrics )
                                                              .hosts( config.getElkHosts().split( ";" ) )
                                                              .index( config.getElkIndex() )
                                                              .indexDateFormat( "YYYY-MM-dd" )
                                                              .filter( ( name, metric ) ->
                                                                       {
                                                                           if ( !name.contains( FILTER_SIMPLE )
                                                                                           && name.contains(
                                                                                           FILTER_JVM ) )
                                                                           {
                                                                               return true;
                                                                           }
                                                                           return false;
                                                                       } )
                                                              .build();

        reporter.start( config.getElkJVMPriod(), TimeUnit.SECONDS );
    }

    private void initELKReporterForHealthCheckMetric( MetricRegistry metrics, IndyMetricsConfig config )
                    throws IOException
    {
        ElasticsearchReporter reporter = ElasticsearchReporter.forRegistry( metrics )
                                                              .hosts( config.getElkHosts().split( ";" ) )
                                                              .index( config.getElkIndex() )
                                                              .indexDateFormat( "YYYY-MM-dd" )
                                                              .filter( ( name, metric ) ->
                                                                       {
                                                                           if ( !name.contains( FILTER_SIMPLE )
                                                                                           && name.contains(
                                                                                           FILTER_HEALTHCHECK ) )
                                                                           {
                                                                               return true;
                                                                           }
                                                                           return false;
                                                                       } )
                                                              .build();

        reporter.start( config.getElkHealthCheckPriod(), TimeUnit.SECONDS );
    }

    private void initConsoleReporter( MetricRegistry metrics, IndyMetricsConfig config )
    {
        ConsoleReporter.forRegistry( metrics )
                       .build()
                       .start( IndyMetricsConfig.INDY_METRICS_REPORTER_GRPHITEREPORTER_DEFAULT_PERIOD,
                               TimeUnit.SECONDS );
    }

    private void initGraphiteReporterForSimpleMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        final Graphite graphite =
                        new Graphite( new InetSocketAddress( config.getGrphiterHostName(), config.getGrphiterPort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGrphiterPrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( ( name, metric ) ->
                                                                   {
                                                                       if ( name.contains( FILTER_SIMPLE ) )
                                                                       {
                                                                           return true;
                                                                       }
                                                                       return false;
                                                                   } )
                                                          .build( graphite );
        reporter.start( config.getGrphiterSimplePriod(), TimeUnit.SECONDS );
    }

    private void initGraphiteReporterForJVMMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        final Graphite graphite =
                        new Graphite( new InetSocketAddress( config.getGrphiterHostName(), config.getGrphiterPort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGrphiterPrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( ( name, metric ) ->
                                                                   {
                                                                       if ( !name.contains( FILTER_SIMPLE )
                                                                                       && name.contains( FILTER_JVM ) )
                                                                       {
                                                                           return true;
                                                                       }
                                                                       return false;
                                                                   } )
                                                          .build( graphite );
        reporter.start( config.getGrphiterJVMPriod(), TimeUnit.SECONDS );
    }

    private void initGraphiteReporterForHealthCheckMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        final Graphite graphite =
                        new Graphite( new InetSocketAddress( config.getGrphiterHostName(), config.getGrphiterPort() ) );
        final GraphiteReporter reporter = GraphiteReporter.forRegistry( metrics )
                                                          .prefixedWith( config.getGrphiterPrefix() )
                                                          .convertRatesTo( TimeUnit.SECONDS )
                                                          .convertDurationsTo( TimeUnit.MILLISECONDS )
                                                          .filter( ( name, metric ) ->
                                                                   {
                                                                       if ( !name.contains( FILTER_SIMPLE )
                                                                                       && name.contains(
                                                                                       FILTER_HEALTHCHECK ) )
                                                                       {
                                                                           return true;
                                                                       }
                                                                       return false;
                                                                   } )
                                                          .build( graphite );
        reporter.start( config.getGrphiterHealthcheckPeriod(), TimeUnit.SECONDS );
    }

    private boolean isExistReporter( String reporter )
    {
        return config.getReporter().contains( reporter );

    }

    private void initZabbixReporterForSimpleMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        IndyZabbixReporter reporter = initZabbixReporter( metrics, config ).filter( ( name, metric ) ->
                                                                                    {
                                                                                        if ( name.contains(
                                                                                                        FILTER_SIMPLE ) )
                                                                                        {
                                                                                            return true;
                                                                                        }
                                                                                        return false;
                                                                                    } ).build( initZabbixSender() );

        reporter.start( config.getZabbixSimplePriod(), TimeUnit.SECONDS );
    }

    private void initZabbixReporterForJVMMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        IndyZabbixReporter reporter = initZabbixReporter( metrics, config ).filter( ( name, metric ) ->
                                                                                    {
                                                                                        if ( !name.contains(
                                                                                                        FILTER_SIMPLE )
                                                                                                        && name.contains(
                                                                                                        FILTER_JVM ) )
                                                                                        {
                                                                                            return true;
                                                                                        }
                                                                                        return false;
                                                                                    } ).build( initZabbixSender() );

        reporter.start( config.getZabbixJVMPriod(), TimeUnit.SECONDS );
    }

    private void initZabbixReporterForHealthCheckMetric( MetricRegistry metrics, IndyMetricsConfig config )
    {
        IndyZabbixReporter reporter = initZabbixReporter( metrics, config ).filter( ( name, metric ) ->
                                                                                    {
                                                                                        if ( !name.contains(
                                                                                                        FILTER_SIMPLE )
                                                                                                        && name.contains(
                                                                                                        FILTER_HEALTHCHECK ) )
                                                                                        {
                                                                                            return true;
                                                                                        }
                                                                                        return false;
                                                                                    } ).build( initZabbixSender() );

        reporter.start( config.getZabbixHealthcheckPeriod(), TimeUnit.SECONDS );
    }

    private IndyZabbixReporter.Builder initZabbixReporter( MetricRegistry metrics, IndyMetricsConfig config )
    {
        return IndyZabbixReporter.forRegistry( metrics )
                                 .prefix( config.getZabbixPrefix() )
                                 .convertRatesTo( TimeUnit.SECONDS )
                                 .convertDurationsTo( TimeUnit.MILLISECONDS )
                                 .hostName( config.getZabbixLocalHostName() );
    }

    private IndyZabbixSender initZabbixSender()
    {
        final IndyZabbixSender zabbixSender = IndyZabbixSender.create()
                                                              .zabbixHost( config.getZabbixHost() )
                                                              .zabbixPort( config.getZabbixPort() )
                                                              .zabbixHostUrl( config.getZabbixApiHostUrl() )
                                                              .zabbixUserName( config.getZabbixUser() )
                                                              .zabbixUserPwd( config.getZabbixPwd() )
                                                              .hostName( config.getZabbixLocalHostName() )
                                                              .bCreateNotExistZabbixSender( true )
                                                              .indyHttpProvider( indyHttpProvider )
                                                              .metricsZabbixCache( cache )
                                                              .build();
        return zabbixSender;
    }
}
