package org.commonjava.indy.subsys.kafka.handler;

import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.commonjava.indy.subsys.kafka.IndyKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;


@ApplicationScoped
public class KafkaEventProducer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    IndyKafkaProducer kafkaProducer;
    public KafkaEventProducer()
    {

    }

    @PostConstruct
    public  void init()
    {

    }


    public void send() throws IOException
    {
        doKafkaSend("first topic ", "hello world");

    }

    public void doKafkaSend( String topic , String message ) throws IOException
    {
        if(kafkaProducer != null){
            kafkaProducer.send(topic,message,null);
        }else{
            logger.debug("Kafka did not start-----------------------");
        }
    }

    @PreDestroy
    public void close() throws IOException
    {
        if ( kafkaProducer != null )
        {
            logger.info( "Closing {}", this.getClass());
            kafkaProducer.close();
        }
    }
}

