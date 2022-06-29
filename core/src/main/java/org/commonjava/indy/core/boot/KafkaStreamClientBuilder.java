/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.core.boot;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.commonjava.indy.action.BootupAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Properties;

@ApplicationScoped
public class KafkaStreamClientBuilder
        implements BootupAction {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

//    @Inject
//    RepoServiceEventHandler repoServiceEventHandler;

    @Override
    public void init() {
        logger.info( "Start build kafka streaming" );

        final Serde<String> stringSerde = Serdes.String();
        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> views = builder.stream(
                "store-event",
                Consumed.with( stringSerde, stringSerde )
        );
//        repoServiceEventHandler.dispatchEvent( views );

        final Properties props = new Properties();
        props.putIfAbsent( StreamsConfig.APPLICATION_ID_CONFIG, "k-streams-group" );
        props.putIfAbsent( StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092" );

        final KafkaStreams streams = new KafkaStreams( builder.build(), props );

        try
        {
            streams.start();
        } catch ( final Throwable e )
        {
            System.exit( 1 );
        }
    }

    @Override
    public String getId() {
        return "kafka streams build";
    }

    @Override
    public int getBootPriority() {
        return 100;
    }
}
