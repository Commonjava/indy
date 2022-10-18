package org.commonjava.indy.subsys.kafka.handler;

import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.util.Properties;


@ApplicationScoped
public class MetadataServiceKafkaProducer {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    static String bootstrapServers = "127.0.0.1:9092";
    private KafkaProducer kafkaProducer;

    public MetadataServiceKafkaProducer() {

    }

    @PostConstruct
        public  void init(){
            Properties properties = new Properties();
            properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            kafkaProducer = new KafkaProducer(properties);
        }


        public void send() throws IOException {
            doKafkaSend("first topic ", "hello world");
        }

        public void doKafkaSend( String topic , String message ) throws IOException {
            if(kafkaProducer != null){
                ProducerRecord<String, String> record =
                        new ProducerRecord<String, String>(topic, message);
                kafkaProducer.send(record);
            }else{
                logger.debug("Kafka did not start-----------------------");
            }
        }

        @PreDestroy
        public void close() throws IOException
        {
            if ( kafkaProducer != null )
            {
                logger.info( "Closing {}", this.getClass() );
                kafkaProducer.close();
            }
        }
}

