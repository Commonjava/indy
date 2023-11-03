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
package org.commonjava.indy.subsys.kafka.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.KafkaTelemetry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.streams.processor.internals.DefaultKafkaClientSupplier;
import org.commonjava.indy.subsys.trace.config.IndyTraceConfiguration;
import org.commonjava.o11yphant.otel.OtelUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class TracingKafkaClientSupplier
        extends DefaultKafkaClientSupplier
{
    @Inject
    IndyTraceConfiguration traceConfiguration;

    @Override
    public Producer<byte[], byte[]> getProducer( Map<String, Object> config )
    {
        KafkaTelemetry telemetry = KafkaTelemetry.create( getOpentelemetry() );
        return telemetry.wrap( super.getProducer( config ) );
    }

    @Override
    public Consumer<byte[], byte[]> getConsumer( Map<String, Object> config )
    {
        KafkaTelemetry telemetry = KafkaTelemetry.create( getOpentelemetry() );
        return telemetry.wrap( super.getConsumer( config ) );
    }

    @Override
    public Consumer<byte[], byte[]> getRestoreConsumer( Map<String, Object> config )
    {
        return this.getConsumer( config );
    }

    @Override
    public Consumer<byte[], byte[]> getGlobalConsumer( Map<String, Object> config )
    {
        return this.getConsumer( config );
    }

    private OpenTelemetry getOpentelemetry()
    {
        return OtelUtil.getOpenTelemetry( traceConfiguration, traceConfiguration );
    }
}
