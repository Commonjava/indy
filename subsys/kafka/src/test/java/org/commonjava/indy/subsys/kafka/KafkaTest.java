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
package org.commonjava.indy.subsys.kafka;

import org.commonjava.indy.subsys.kafka.util.LogbackFormatter;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

public class KafkaTest
{
    @Ignore
    @Test
    public void logbackFormatterTest() throws IOException
    {
        LogbackFormatter formatter = new LogbackFormatter( "org.commonjava.indy._userlog_.kafka-log" );
        String msg = formatter.format( "Hello world!" );

        System.out.println( msg );
        //{"timestamp":"2018-12-20T04:28:18.551","level":"INFO","thread":"main","logger":"org.commonjava.indy._userlog_.kafka-log","message":"Hello world!","context":"default"}
    }

    /**
     * To run this, follow https://kafka.apache.org/quickstart until step 3 (create the topic).
     * If you want to view the message on console, follow step 5.
     */
    @Ignore
    @Test
    public void sendMessageTest() throws Exception
    {
        LogbackFormatter formatter = new LogbackFormatter( "org.commonjava.indy._userlog_.kafka-log" );

        Properties props = new Properties();
        props.load( getClass().getClassLoader().getResourceAsStream( "producer.properties" ) );
        IndyKafkaProducer kafkaProducer = new IndyKafkaProducer( props );

        kafkaProducer.send( "test", "Hello world!", formatter );
        kafkaProducer.send( "test", "This is KafkaTest!", formatter, 30000 );

        kafkaProducer.close();
    }

}
