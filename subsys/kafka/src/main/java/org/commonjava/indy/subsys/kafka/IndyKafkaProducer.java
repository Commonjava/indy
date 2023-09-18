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
package org.commonjava.indy.subsys.kafka;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.KafkaTelemetry;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.commonjava.indy.subsys.kafka.conf.KafkaConfig;
import org.commonjava.indy.subsys.trace.config.IndyTraceConfiguration;
import org.commonjava.o11yphant.otel.OtelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@SuppressWarnings( { "unused", "rawtypes" } )
@ApplicationScoped
public class IndyKafkaProducer
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private KafkaConfig config;

    @Inject
    IndyTraceConfiguration traceConfiguration;

    private Producer<String, Object> kafkaProducer;

    private final Consumer<Exception> exceptionHandler = ( e ) -> logger.error( "Send to Kafka failed", e );

    private final Callback callback = ( metadata, exception ) -> {
        if ( exception != null )
        {
            exceptionHandler.accept( exception );
        }
        else
        {
            logger.trace( "Message sent to Kafka. Partition:{}, timestamp {}.", metadata.partition(),
                          metadata.timestamp() );
        }
    };

    @PostConstruct
    private void init()
    {
        if ( config.isEnabled() )
        {
            Properties props = new Properties();
            props.setProperty( ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers() );
            props.setProperty( ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName() );
            props.setProperty( ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                               KafkaObjectMapperSerializer.class.getName() );
            kafkaProducer = new KafkaProducer<>( props );
            if ( traceConfiguration.isEnabled() && config.isTracing() )
            {
                logger.info( "Enabling the opentelemetry for Kafka message producer." );
                final OpenTelemetry otel = OtelUtil.getOpenTelemetry( traceConfiguration, traceConfiguration );
                final KafkaTelemetry telemetry = KafkaTelemetry.create( otel );
                kafkaProducer = telemetry.wrap( kafkaProducer );
            }
        }
    }

    /**
     * Non-blocking send. The message will not be really available to consumers until flush()
     * or close() is called, or until another blocking send is called.
     */
    public void send( String topic, Object message )
    {
        doKafkaSend( topic, message );
    }

    /**
     * Blocking send. The message will be available to consumers immediately. Wait for at most the given time
     * for the operation to complete.
     */
    public void send( String topic, Object message, long timeoutMillis )
            throws InterruptedException, ExecutionException, TimeoutException
    {
        Future future = doKafkaSend( topic, message );
        if ( future != null )
        {
            future.get( timeoutMillis, TimeUnit.MILLISECONDS );
        }
    }

    private Future doKafkaSend( String topic, Object message )
    {
        if ( kafkaProducer != null )
        {
            ProducerRecord<String, Object> producerRecord = new ProducerRecord<>( topic, message );

            return kafkaProducer.send( producerRecord, callback );
        }
        return null;
    }

    /**
     * Flush all the non-blocking send messages so that the consumers can see them.
     */
    public void flush()
    {
        if ( kafkaProducer != null )
        {
            kafkaProducer.flush();
        }
    }

    @PreDestroy
    public void close()
    {
        if ( kafkaProducer != null )
        {
            logger.info( "Closing {}", this.getClass() );
            kafkaProducer.close();
        }
    }

}
