/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.kafka.boot;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.subsys.kafka.conf.KafkaConfig;
import org.commonjava.indy.subsys.kafka.handler.ServiceEventHandler;
import org.commonjava.indy.subsys.kafka.trace.TracingKafkaClientSupplier;
import org.commonjava.indy.subsys.trace.config.IndyTraceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Properties;

@ApplicationScoped
public class KafkaStreamBooter
                implements BootupAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    Instance<ServiceEventHandler> serviceEventHandlers;

    @Inject
    KafkaConfig config;
    
    @Inject
    IndyTraceConfiguration traceConfig;

    @Override
    public void init() throws IndyLifecycleException
    {

        if ( !config.isEnabled() )
        {
            logger.warn( "Kafka stream is disabled, this will effect communicating with the microservices." );
            return;
        }
        logger.info( "Start init kafka streaming" );

        final Serde<String> stringSerde = Serdes.String();
        final StreamsBuilder builder = new StreamsBuilder();

        if ( config.getTopics() == null )
        {
            logger.warn( "Don't find topics configured in config, will not start kafka consumer streaming." );
            return;
        }
        for ( String topic : config.getTopics() )
        {
            KStream<String, String> stream = builder.stream( topic, Consumed.with( stringSerde, stringSerde ) );
            for (ServiceEventHandler handler : serviceEventHandlers)
            {
                if ( handler.canHandle(topic) )
                {
                    handler.dispatchEvent( stream, topic );
                }
            }
        }

        Properties props = setKafkaProps();

        if ( traceConfig.isEnabled() )
        {
            logger.info("The trace is enabled for Kafka client, so inject otel instrumentation to Kafka client.");
            try (final KafkaStreams streams = new KafkaStreams( builder.build(), props,
                                                                new TracingKafkaClientSupplier() ))
            {
                streams.start();
            }
            catch ( final Throwable e )
            {
                throw new IndyLifecycleException( "Failed to start Kafka consumer streaming.", e );
            }
        }
        else
        {
            try (final KafkaStreams streams = new KafkaStreams( builder.build(), props ))
            {
                streams.start();
            }
            catch ( final Throwable e )
            {
                throw new IndyLifecycleException( "Failed to start Kafka consumer streaming.", e );
            }
        }
    }

    @Override
    public String getId()
    {
        return "kafka streaming boot";
    }

    @Override
    public int getBootPriority()
    {
        return 100;
    }

    private Properties setKafkaProps()
    {
        final Properties props = new Properties();
        props.putIfAbsent( StreamsConfig.APPLICATION_ID_CONFIG, config.getGroup() );
        props.putIfAbsent( StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers() );
        props.putIfAbsent( StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, config.getRecordsPerPartition() );

        logger.info( "Kafka props: {}", props );
        return props;
    }
}