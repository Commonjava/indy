/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.metrics.zabbix.reporter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.commonjava.indy.metrics.exception.IndyMetricsException;
import org.commonjava.indy.metrics.zabbix.sender.DataObject;
import org.commonjava.indy.metrics.zabbix.sender.IndyZabbixSender;
import org.commonjava.indy.metrics.zabbix.sender.SenderResult;
import org.commonjava.indy.subsys.http.IndyHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiabai on 3/29/17.
 */
public class IndyZabbixReporter
                extends ScheduledReporter
{
    private static final Logger logger = LoggerFactory.getLogger( IndyZabbixReporter.class );

    private final IndyZabbixSender indyZabbixSender;

    private final String hostName;

    private final String prefix;

    private final String suffix;

    public static Builder forRegistry( MetricRegistry registry )
    {
        return new Builder( registry );
    }

    public static class Builder
    {

        private final MetricRegistry registry;

        private String name = "zabbix-reporter";

        private TimeUnit rateUnit;

        private TimeUnit durationUnit;

        private MetricFilter filter;

        private String hostName;

        private String prefix = "";

        private String suffix = "";

        private String zabbixHostName;

        private IndyZabbixSender indyZabbixSender;

        public Builder( MetricRegistry registry )
        {
            this.registry = registry;

            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;

        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit
         *            a unit of time
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
         * @param durationUnit
         *            a unit of time
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
         * @param filter
         *            a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter( MetricFilter filter )
        {
            this.filter = filter;
            return this;
        }

        /**
         * default register name is "zabbix-reporter".
         *
         * @param name
         * @return
         */
        public Builder name( String name )
        {
            this.name = name;
            return this;
        }

        public Builder hostName( String hostName )
        {
            this.hostName = hostName;
            return this;
        }

        public Builder prefix( String prefix )
        {
            this.prefix = prefix + ".";
            return this;
        }

        public Builder suffix( String suffix )
        {
            this.suffix = suffix;
            return this;
        }

        public Builder zabbixHostName( String zabbixHostName )
        {
            this.zabbixHostName = zabbixHostName;
            return this;
        }

        public Builder zabbixSender( IndyZabbixSender indyZabbixSender )
        {
            this.indyZabbixSender = indyZabbixSender;
            return this;
        }

        /**
         * Builds a {@link IndyZabbixReporter} with the given properties.
         *
         * @return a {@link IndyZabbixReporter}
         */
        public IndyZabbixReporter build( IndyZabbixSender zabbixSender )
        {
            if ( hostName == null )
            {
                hostName = HostUtil.getHostName();
                logger.info( name + " detect hostName: " + hostName );
            }
            return new IndyZabbixReporter( registry, name, rateUnit, durationUnit, filter, zabbixSender, hostName,
                                           prefix, suffix );
        }
    }

    private IndyZabbixReporter( MetricRegistry registry, String name, TimeUnit rateUnit, TimeUnit durationUnit,
                                MetricFilter filter, IndyZabbixSender indyZabbixSender, String hostName, String prefix,
                                String suffix )
    {
        super( registry, name, filter, rateUnit, durationUnit );
        this.indyZabbixSender = indyZabbixSender;
        this.hostName = hostName;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    private DataObject toDataObject( String key, String keySuffix, Object value, long clock )
    {
        return DataObject.builder()
                         .host( hostName )
                         .key( prefix + key + keySuffix + suffix )
                         .clock( clock )
                         .value( String.valueOf( value ) )
                         .build();
    }

    /**
     * for histograms.
     *
     * @param key
     * @param snapshot
     * @param dataObjectList
     */
    private void addSnapshotDataObject( String key, Snapshot snapshot, long clock, List<DataObject> dataObjectList )
    {
        dataObjectList.add( toDataObject( key, ".min", snapshot.getMin(), clock ) );
        dataObjectList.add( toDataObject( key, ".max", snapshot.getMax(), clock ) );
        dataObjectList.add( toDataObject( key, ".mean", snapshot.getMean(), clock ) );
        dataObjectList.add( toDataObject( key, ".stddev", snapshot.getStdDev(), clock ) );
        dataObjectList.add( toDataObject( key, ".median", snapshot.getMedian(), clock ) );
        dataObjectList.add( toDataObject( key, ".75th", snapshot.get75thPercentile(), clock ) );
        dataObjectList.add( toDataObject( key, ".95th", snapshot.get95thPercentile(), clock ) );
        dataObjectList.add( toDataObject( key, ".98th", snapshot.get98thPercentile(), clock ) );
        dataObjectList.add( toDataObject( key, ".99th", snapshot.get99thPercentile(), clock ) );
        dataObjectList.add( toDataObject( key, ".99.9th", snapshot.get999thPercentile(), clock ) );
    }

    /**
     * for timer.
     *
     * @param key
     * @param snapshot
     * @param dataObjectList
     */
    private void addSnapshotDataObjectWithConvertDuration( String key, Snapshot snapshot, long clock,
                                                           List<DataObject> dataObjectList )
    {
        dataObjectList.add( toDataObject( key, ".min", convertDuration( snapshot.getMin() ), clock ) );
        dataObjectList.add( toDataObject( key, ".max", convertDuration( snapshot.getMax() ), clock ) );
        dataObjectList.add( toDataObject( key, ".mean", convertDuration( snapshot.getMean() ), clock ) );
        dataObjectList.add( toDataObject( key, ".stddev", convertDuration( snapshot.getStdDev() ), clock ) );
        dataObjectList.add( toDataObject( key, ".median", convertDuration( snapshot.getMedian() ), clock ) );
        dataObjectList.add( toDataObject( key, ".75th", convertDuration( snapshot.get75thPercentile() ), clock ) );
        dataObjectList.add( toDataObject( key, ".95th", convertDuration( snapshot.get95thPercentile() ), clock ) );
        dataObjectList.add( toDataObject( key, ".98th", convertDuration( snapshot.get98thPercentile() ), clock ) );
        dataObjectList.add( toDataObject( key, ".99th", convertDuration( snapshot.get99thPercentile() ), clock ) );
        dataObjectList.add( toDataObject( key, ".99.9th", convertDuration( snapshot.get999thPercentile() ), clock ) );
    }

    private void addMeterDataObject( String key, Metered meter, long clock, List<DataObject> dataObjectList )
    {
        dataObjectList.add( toDataObject( key, ".count", meter.getCount(), clock ) );
        dataObjectList.add( toDataObject( key, ".meanRate", convertRate( meter.getMeanRate() ), clock ) );
        dataObjectList.add( toDataObject( key, ".1-minuteRate", convertRate( meter.getOneMinuteRate() ), clock ) );
        dataObjectList.add( toDataObject( key, ".5-minuteRate", convertRate( meter.getFiveMinuteRate() ), clock ) );
        dataObjectList.add( toDataObject( key, ".15-minuteRate", convertRate( meter.getFifteenMinuteRate() ), clock ) );
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public void report( SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                        SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                        SortedMap<String, Timer> timers )
    {
        final long clock = System.currentTimeMillis() / 1000;
        List<DataObject> dataObjectList = new LinkedList<DataObject>();
        for ( Map.Entry<String, Gauge> entry : gauges.entrySet() )
        {
            DataObject dataObject =
                            toDataObject( entry.getKey(), "", String.valueOf( entry.getValue().getValue() ), clock );
            dataObjectList.add( dataObject );
        }

        for ( Map.Entry<String, Counter> entry : counters.entrySet() )
        {
            DataObject dataObject =
                            toDataObject( entry.getKey(), "", String.valueOf( entry.getValue().getCount() ), clock );
            dataObjectList.add( dataObject );
        }

        for ( Map.Entry<String, Histogram> entry : histograms.entrySet() )
        {
            Histogram histogram = entry.getValue();
            Snapshot snapshot = histogram.getSnapshot();
            addSnapshotDataObject( entry.getKey(), snapshot, clock, dataObjectList );
        }

        for ( Map.Entry<String, Meter> entry : meters.entrySet() )
        {
            Meter meter = entry.getValue();
            addMeterDataObject( entry.getKey(), meter, clock, dataObjectList );
        }

        for ( Map.Entry<String, Timer> entry : timers.entrySet() )
        {
            Timer timer = entry.getValue();
            addMeterDataObject( entry.getKey(), timer, clock, dataObjectList );
            addSnapshotDataObjectWithConvertDuration( entry.getKey(), timer.getSnapshot(), clock, dataObjectList );
        }

        try
        {
            SenderResult senderResult = indyZabbixSender.send( dataObjectList, clock );
            if ( !senderResult.success() )
            {
                logger.warn( "report metrics to zabbix not success!" + senderResult );
            }
            else if ( logger.isDebugEnabled() )
            {
                logger.info( "report metrics to zabbix success. " + senderResult );
            }
        }
        catch ( IOException e )
        {
            logger.error( "report metrics to zabbix error! " + e );
            e.printStackTrace();
        }
        catch ( IndyMetricsException e )
        {
            logger.error( "Indy metrics config error " + e );
            e.printStackTrace();
        }
        catch ( IndyHttpException e )
        {
            logger.error( "Indy http client error " + e );
            e.printStackTrace();
        }
    }

}
