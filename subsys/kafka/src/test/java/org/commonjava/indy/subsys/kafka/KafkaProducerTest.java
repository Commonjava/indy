package org.commonjava.indy.subsys.kafka;

import jnr.ffi.annotations.Meta;


import org.commonjava.indy.subsys.kafka.handler.MetadataServiceKafkaProducer;
import org.commonjava.indy.subsys.kafka.handler.RepoServiceEventHandler;
import org.commonjava.indy.subsys.kafka.util.LogbackFormatter;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class KafkaProducerTest  {
    @Test
    public void fff() throws IOException {
        MetadataServiceKafkaProducer producer = new MetadataServiceKafkaProducer();
        producer.send();
    }
}
