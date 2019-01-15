/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.metrics.context.reporter;

import com.codahale.metrics.Clock;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.metrics.context.ContextMetric;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContextConsoleJsonReporter
        extends ScheduledReporter
{
    public static class MetricKey
    {
        public static final String NAME = "name";

        public static final String COUNT = "count";

        public static final String MEAN_RATE = "mean-rate";

        public static final String M1_RATE = "m1-rate";

        public static final String M5_RATE = "m5-rate";

        public static final String M15_RATE = "m15-rate";

        public static final String MIN = "min";

        public static final String MAX = "max";

        public static final String MEAN = "mean";

        public static final String STDDEV = "stddev";

        public static final String P50 = "median";

        public static final String P75 = "75%";

        public static final String P95 = "95%";

        public static final String P98 = "98%";

        public static final String P99 = "99%";

        public static final String P999 = "99.9%";

    }

    /**
     * Returns a new {@link Builder} for {@link ConsoleReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link ConsoleReporter}
     */
    public static Builder forRegistry( MetricRegistry registry )
    {
        return new Builder( registry );
    }

    /**
     * A builder for {@link ConsoleReporter} instances. Defaults to using the default locale and
     * time zone, writing to {@code System.out}, converting rates to events/second, converting
     * durations to milliseconds, and not filtering metrics.
     */
    public static class Builder
    {
        private final MetricRegistry registry;

        private PrintStream output;

        private Locale locale;

        private Clock clock;

        private TimeZone timeZone;

        private TimeUnit rateUnit;

        private TimeUnit durationUnit;

        private MetricFilter filter;

        private ScheduledExecutorService executor;

        private boolean shutdownExecutorOnStop;

        private Set<MetricAttribute> disabledMetricAttributes;

        private Builder( MetricRegistry registry )
        {
            this.registry = registry;
            this.output = System.out;
            this.locale = Locale.getDefault();
            this.clock = Clock.defaultClock();
            this.timeZone = TimeZone.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.executor = null;
            this.shutdownExecutorOnStop = true;
            disabledMetricAttributes = Collections.emptySet();
        }

        /**
         * Specifies whether or not, the executor (used for reporting) will be stopped with same time with reporter.
         * Default value is true.
         * Setting this parameter to false, has the sense in combining with providing external managed executor via {@link #scheduleOn(ScheduledExecutorService)}.
         *
         * @param shutdownExecutorOnStop if true, then executor will be stopped in same time with this reporter
         * @return {@code this}
         */
        public Builder shutdownExecutorOnStop( boolean shutdownExecutorOnStop )
        {
            this.shutdownExecutorOnStop = shutdownExecutorOnStop;
            return this;
        }

        /**
         * Specifies the executor to use while scheduling reporting of metrics.
         * Default value is null.
         * Null value leads to executor will be auto created on start.
         *
         * @param executor the executor to use while scheduling reporting of metrics.
         * @return {@code this}
         */
        public Builder scheduleOn( ScheduledExecutorService executor )
        {
            this.executor = executor;
            return this;
        }

        /**
         * Write to the given {@link PrintStream}.
         *
         * @param output a {@link PrintStream} instance.
         * @return {@code this}
         */
        public Builder outputTo( PrintStream output )
        {
            this.output = output;
            return this;
        }

        /**
         * Format numbers for the given {@link Locale}.
         *
         * @param locale a {@link Locale}
         * @return {@code this}
         */
        public Builder formattedFor( Locale locale )
        {
            this.locale = locale;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock( Clock clock )
        {
            this.clock = clock;
            return this;
        }

        /**
         * Use the given {@link TimeZone} for the time.
         *
         * @param timeZone a {@link TimeZone}
         * @return {@code this}
         */
        public Builder formattedFor( TimeZone timeZone )
        {
            this.timeZone = timeZone;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo( TimeUnit rateUnit )
        {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo( TimeUnit durationUnit )
        {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter( MetricFilter filter )
        {
            this.filter = filter;
            return this;
        }

        /**
         * Don't report the passed metric attributes for all metrics (e.g. "p999", MetricKey.STDDEV or "m15").
         * See {@link MetricAttribute}.
         *
         * @param disabledMetricAttributes a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder disabledMetricAttributes( Set<MetricAttribute> disabledMetricAttributes )
        {
            this.disabledMetricAttributes = disabledMetricAttributes;
            return this;
        }

        /**
         * Builds a {@link ConsoleReporter} with the given properties.
         *
         * @return a {@link ConsoleReporter}
         */
        public ContextConsoleJsonReporter build()
        {
            return new ContextConsoleJsonReporter( registry, output, locale, clock, timeZone, rateUnit, durationUnit,
                                                   filter, executor, shutdownExecutorOnStop, disabledMetricAttributes );
        }
    }

    private static final int CONSOLE_WIDTH = 80;

    private final PrintStream output;

    private final Locale locale;

    private final Clock clock;

    private final DateFormat dateFormat;

    private final ObjectMapper mapper = new ObjectMapper();

    private ContextConsoleJsonReporter( MetricRegistry registry, PrintStream output, Locale locale, Clock clock,
                                        TimeZone timeZone, TimeUnit rateUnit, TimeUnit durationUnit,
                                        MetricFilter filter, ScheduledExecutorService executor,
                                        boolean shutdownExecutorOnStop, Set<MetricAttribute> disabledMetricAttributes )
    {
        super( registry, "console-reporter", filter, rateUnit, durationUnit, executor, shutdownExecutorOnStop,
               disabledMetricAttributes );
        this.output = output;
        this.locale = locale;
        this.clock = clock;
        this.dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.MEDIUM, locale );
        dateFormat.setTimeZone( timeZone );
    }

    @Override
    @SuppressWarnings( "rawtypes" )
    public void report( SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                        SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                        SortedMap<String, Timer> timers )
    {
        final String dateTime = dateFormat.format( new Date( clock.getTime() ) );
        printWithBanner( dateTime, '=' );
        output.println();

        if ( !gauges.isEmpty() )
        {
            printWithBanner( "-- Gauges", '-' );
            for ( Map.Entry<String, Gauge> entry : gauges.entrySet() )
            {
                printGauge( entry.getKey(), entry.getValue() );
            }
            output.println();
        }

        if ( !counters.isEmpty() )
        {
            printWithBanner( "-- Counters", '-' );
            for ( Map.Entry<String, Counter> entry : counters.entrySet() )
            {
                printCounter( entry.getKey(), entry.getValue() );
            }
            output.println();
        }

        if ( !histograms.isEmpty() )
        {
            printWithBanner( "-- Histograms", '-' );
            for ( Map.Entry<String, Histogram> entry : histograms.entrySet() )
            {
                printHistogram( entry.getKey(), entry.getValue() );
            }
            output.println();
        }

        if ( !meters.isEmpty() )
        {
            printWithBanner( "-- Meters", '-' );
            for ( Map.Entry<String, Meter> entry : meters.entrySet() )
            {
                printMeter( entry.getKey(), entry.getValue() );
            }
            output.println();
        }

        if ( !timers.isEmpty() )
        {
            printWithBanner( "-- Timers", '-' );
            for ( Map.Entry<String, Timer> entry : timers.entrySet() )
            {
                printTimer( entry.getKey(), entry.getValue() );
            }
            output.println();
        }

        output.println();
        output.flush();
    }

    private void printMeter( String name, Meter meter )
    {
        final Map<String, Object> meterMap = new HashMap<>();
        meterMap.put( MetricKey.NAME, name );
        putIfEnabled( meterMap, MetricAttribute.COUNT, MetricKey.COUNT, meter.getCount() );
        putIfEnabled( meterMap, MetricAttribute.MEAN_RATE, MetricKey.MEAN_RATE,
                      String.format( locale, "%2.2f events/%s", convertRate( meter.getMeanRate() ), getRateUnit() ) );
        putIfEnabled( meterMap, MetricAttribute.M1_RATE, MetricKey.M1_RATE,
                      String.format( locale, "%2.2f events/%s", convertRate( meter.getOneMinuteRate() ),
                                     getRateUnit() ) );
        putIfEnabled( meterMap, MetricAttribute.M5_RATE, MetricKey.M5_RATE,
                      String.format( locale, "%2.2f events/%s", convertRate( meter.getFiveMinuteRate() ),
                                     getRateUnit() ) );
        putIfEnabled( meterMap, MetricAttribute.M15_RATE, MetricKey.M15_RATE,
                      String.format( locale, "%2.2f events/%s", convertRate( meter.getFifteenMinuteRate() ),
                                     getRateUnit() ) );

        addContext( meter, meterMap );
        printMetricMap( meterMap );

    }

    private void printCounter( String name, Counter counter )
    {
        final Map<String, Object> counterMap = new HashMap<>();
        counterMap.put( MetricKey.NAME, name );
        counterMap.put( MetricKey.COUNT, String.format( locale, "%d%n", counter.getCount() ) );
        addContext( counter, counterMap );
        printMetricMap( counterMap );
    }

    private void printGauge( String name, Gauge<?> gauge )
    {
        final Map<String, Object> gaugeMap = new HashMap<>();
        gaugeMap.put( MetricKey.NAME, name );
        gaugeMap.put( "value", String.format( locale, "%s%n", gauge.getValue() ) );
        addContext( gauge, gaugeMap );
        printMetricMap( gaugeMap );
    }

    private void printHistogram( String name, Histogram histogram )
    {
        final Map<String, Object> histogramMap = new HashMap<>();
        histogramMap.put( MetricKey.NAME, name );
        putIfEnabled( histogramMap, MetricAttribute.COUNT, MetricKey.COUNT,
                      String.format( locale, "%d", histogram.getCount() ) );
        Snapshot snapshot = histogram.getSnapshot();
        putIfEnabled( histogramMap, MetricAttribute.MIN, MetricKey.MIN,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMin() ), getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.MAX, MetricKey.MAX,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMax() ), getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.MEAN, MetricKey.MEAN,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMean() ), getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.STDDEV, MetricKey.STDDEV,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getStdDev() ), getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.P50, MetricKey.P50,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMedian() ), getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.P75, MetricKey.P75,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get75thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.P95, MetricKey.P95,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get95thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.P98, MetricKey.P98,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get98thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.P99, MetricKey.P99,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get99thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( histogramMap, MetricAttribute.P999, MetricKey.P999,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get999thPercentile() ),
                                     getDurationUnit() ) );

        addContext( histogram, histogramMap );
        printMetricMap( histogramMap );
    }

    private void printTimer( String name, Timer timer )
    {
        final Snapshot snapshot = timer.getSnapshot();
        final Map<String, Object> timerMap = new HashMap<>();
        timerMap.put( MetricKey.NAME, name );
        putIfEnabled( timerMap, MetricAttribute.COUNT, MetricKey.COUNT, timer.getCount() );
        putIfEnabled( timerMap, MetricAttribute.MEAN_RATE, MetricKey.MEAN_RATE,
                      String.format( locale, "%2.2f calls/%s", convertRate( timer.getMeanRate() ), getRateUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.M1_RATE, MetricKey.M1_RATE,
                      String.format( locale, "%2.2f calls/%s", convertRate( timer.getOneMinuteRate() ),
                                     getRateUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.M5_RATE, MetricKey.M5_RATE,
                      String.format( locale, "%2.2f calls/%s", convertRate( timer.getFiveMinuteRate() ),
                                     getRateUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.M15_RATE, MetricKey.M15_RATE,
                      String.format( locale, "%2.2f calls/%s", convertRate( timer.getFifteenMinuteRate() ),
                                     getRateUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.MIN, MetricKey.MIN,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMin() ), getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.MAX, MetricKey.MAX,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMax() ), getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.MEAN, MetricKey.MEAN,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMean() ), getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.STDDEV, MetricKey.STDDEV,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getStdDev() ), getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.P50, MetricKey.P50,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.getMedian() ), getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.P75, MetricKey.P75,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get75thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.P95, MetricKey.P95,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get95thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.P98, MetricKey.P98,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get98thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.P99, MetricKey.P99,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get99thPercentile() ),
                                     getDurationUnit() ) );
        putIfEnabled( timerMap, MetricAttribute.P999, MetricKey.P999,
                      String.format( locale, "%2.2f %s", convertDuration( snapshot.get999thPercentile() ),
                                     getDurationUnit() ) );

        addContext( timer, timerMap );
        printMetricMap( timerMap );

    }

    private void addContext( Metric metric, Map<String, Object> metricMap )
    {
        if ( metric instanceof ContextMetric )
        {
            ContextMetric contextMetric = (ContextMetric) metric;
            contextMetric.getContextMap().forEach( metricMap::put );
        }
    }

    private void printMetricMap( Map<String, Object> metricMap )
    {
        try
        {
            output.println( mapper.writeValueAsString( metricMap ) );
        }
        catch ( JsonProcessingException e )
        {
            e.printStackTrace();
        }
    }

    private void printWithBanner( String s, char c )
    {
        output.print( s );
        output.print( ' ' );
        for ( int i = 0; i < ( CONSOLE_WIDTH - s.length() - 1 ); i++ )
        {
            output.print( c );
        }
        output.println();
    }

    /**
     * Print only if the attribute is enabled
     *
     * @param type   Metric attribute
     * @param status Status to be logged
     */
    private void printIfEnabled( MetricAttribute type, String status )
    {
        if ( getDisabledMetricAttributes().contains( type ) )
        {
            return;
        }

        output.println( status );
    }

    private void putIfEnabled( Map<String, Object> map, MetricAttribute type, String statusKey, Object statusValue )
    {
        if ( getDisabledMetricAttributes().contains( type ) )
        {
            return;
        }

        map.put( statusKey, statusValue );
    }
}
