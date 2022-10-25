package org.commonjava.indy.subsys.kafka.handler;

import java.io.IOException;

public interface KafkaEventProducer {
    void init();
    void send() throws  IOException;
    void close() throws IOException;
}
