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
package org.commonjava.indy.folo.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@SectionName( "folo" )
@ApplicationScoped
public class FoloConfig
    implements IndyConfigInfo
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_DISABLED =  false;
    public static final String DEFAULT_FOLO_CASSANDRA_KEYSPACE =  "folo";
    public static final String DEFAULT_FOLO_CASSANDRA_TABLENAME =  "records";

    private Boolean enabled;

    private Boolean storeToCassandra;

    private Boolean trackGroupContent;

    private String foloCassandraKeyspace;

    private String foloCassandraTablename;

    public FoloConfig()
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

    @ConfigName( "enabled")
    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public boolean isGroupContentTracked()
    {
        return Boolean.TRUE.equals( trackGroupContent );
    }

    @ConfigName( "track.group.content" )
    public void setTrackGroupContent( final Boolean trackGroupContent )
    {
        this.trackGroupContent = trackGroupContent;
    }

    public Boolean getTrackGroupContent()
    {
        return trackGroupContent;
    }

    public String getFoloCassandraKeyspace() {
        return foloCassandraKeyspace == null ?  DEFAULT_FOLO_CASSANDRA_KEYSPACE : foloCassandraKeyspace;
    }

    @ConfigName("folo.cassandra.keyspace")
    public void setFoloCassandraKeyspace(String foloCassandraKeyspace) {
        logger.warn("\n\n-- SETING FOLO KEYSPACE NAME: "  + foloCassandraKeyspace);
        this.foloCassandraKeyspace = foloCassandraKeyspace;
    }

    public String getFoloCassandraTablename() {
        return foloCassandraTablename == null ? DEFAULT_FOLO_CASSANDRA_TABLENAME : foloCassandraTablename;
    }

    @ConfigName("folo.cassandra.tablename")
    public void setFoloCassandraTablename(String foloCassandraTablename) {
        this.foloCassandraTablename = foloCassandraTablename;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "folo.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-folo.conf" );
    }

}
