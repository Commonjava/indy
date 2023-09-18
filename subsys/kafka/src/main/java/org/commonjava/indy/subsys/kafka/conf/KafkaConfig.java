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
package org.commonjava.indy.subsys.kafka.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

@SectionName( "kafka" )
@ApplicationScoped
public class KafkaConfig
        implements IndyConfigInfo
{

    private static final String DEFAULT_BOOTSTRP_SERVERS = "127.0.0.1:9092";

    private static final String DEFAULT_GROUP = "kstreams-group";

    private static final Integer DEFAULT_RECORDS_PER_PARTITION = 1000;

    private static final boolean DEFAULT_ENABLED = true;

    private static final boolean DEFAULT_TRACE = false;

    private Boolean enabled;

    private String bootstrapServers;

    private List<String> topics;

    private String group;

    private Integer recordsPerPartition;

    private String fileEventTopic;

    private Boolean tracing;

    public KafkaConfig()
    {
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public Boolean getEnabled()
    {
        return enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public String getBootstrapServers()
    {
        return bootstrapServers == null ? DEFAULT_BOOTSTRP_SERVERS : bootstrapServers;
    }

    @ConfigName( "kafka.bootstrap.servers" )
    public void setBootstrapServers( final String bootstrapServers )
    {
        this.bootstrapServers = bootstrapServers;
    }

    public List<String> getTopics()
    {
        return topics;
    }

    @ConfigName( "kafka.topics" )
    public void setTopics( final String topic )
    {
        String[] topicArray = topic.split( "," );
        this.topics = new ArrayList<>();
        this.topics.addAll( Arrays.asList( topicArray ) );
    }

    public String getGroup()
    {
        String group = System.getenv( "POD_NAME" );
        return isBlank( group ) ? DEFAULT_GROUP : group;
    }

    public void setGroup( final String group )
    {
        this.group = group;
    }

    public Integer getRecordsPerPartition()
    {
        return recordsPerPartition == null ? DEFAULT_RECORDS_PER_PARTITION : recordsPerPartition;
    }

    @ConfigName( "kafka.records.per.partition" )
    public void setRecordsPerPartition( Integer recordsPerPartition )
    {
        this.recordsPerPartition = recordsPerPartition;
    }

    public String getFileEventTopic()
    {
        return fileEventTopic;
    }

    @ConfigName( "topic.file_event" )
    public void setFileEventTopic( String fileEventTopic )
    {
        this.fileEventTopic = fileEventTopic;
    }

    public boolean isTracing()
    {
        return tracing == null ? DEFAULT_TRACE : tracing;
    }

    @ConfigName( "kafka.trace" )
    public void setTracing( boolean tracing )
    {
        this.tracing = tracing;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "kafka.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-kafka.conf" );
    }
}
