package org.commonjava.indy.subsys.kafka;


import io.reactivex.Completable;
import org.commonjava.indy.subsys.kafka.handler.KafkaEventProducer;
import org.junit.Test;

import java.io.IOException;

public class KafkaProducerTest  {
    @Test
    public void sendMessageTest() throws IOException {
        KafkaEventProducer kafkaEventProducer = new KafkaEventProducer();
        kafkaEventProducer.send();
        kafkaEventProducer.close();
    }
}
