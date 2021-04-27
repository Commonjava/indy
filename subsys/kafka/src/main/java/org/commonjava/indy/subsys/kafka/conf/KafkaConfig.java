/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@SectionName( "kafka" )
@ApplicationScoped
public class KafkaConfig extends MapSectionListener
        implements IndyConfigInfo
{

    private static final boolean DEFAULT_ENABLED = false;

    private static final long DEFALUT_TIMEOUTMILLIS = 60000;

    private Boolean enabled;

    private String fileEventTopic;

    private Long timeoutMillis;

    public KafkaConfig()
    {
    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
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

    @Override
    public void sectionComplete(String name) throws ConfigurationException
    {
        String s = getConfiguration().get( "enabled" );
        if ( s != null)
        {
            this.enabled = Boolean.parseBoolean( s );
        }
    }

    public Long getTimeoutMillis()
    {
        return timeoutMillis == null ? DEFALUT_TIMEOUTMILLIS : timeoutMillis ;
    }

    @ConfigName( "timeout_in_mills" )
    public void setTimeoutMillis( Long timeoutMillis )
    {
        this.timeoutMillis = timeoutMillis;
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
