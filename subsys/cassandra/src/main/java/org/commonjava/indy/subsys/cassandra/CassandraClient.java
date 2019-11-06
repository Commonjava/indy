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
package org.commonjava.indy.subsys.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.commonjava.indy.subsys.cassandra.config.CassandraConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@ApplicationScoped
public class CassandraClient
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CassandraConfig config;

    private Session session;

    public CassandraClient()
    {
    }

    public CassandraClient( CassandraConfig config )
    {
        this.config = config;
        init();
    }

    @PostConstruct
    private void init()
    {
        if ( !config.isEnabled() )
        {
            logger.debug( "Cassandra client not enabled" );
            return;
        }
        try
        {
            String host = config.getCassandraHost();
            int port = config.getCassandraPort();
            Cluster.Builder builder = Cluster.builder().withoutJMXReporting().addContactPoint( host ).withPort( port );
            String username = config.getCassandraUser();
            String password = config.getCassandraPass();
            if ( isNotBlank( username ) && isNotBlank( password ) )
            {
                logger.debug( "Build with credentials, user: {}, pass: ****", username );
                builder.withCredentials( username, password );
            }
            Cluster cluster = builder.build();

            logger.debug( "Connecting to Cassandra, host:{}, port:{}", host, port );
            session = cluster.connect();
        }
        catch ( Exception e )
        {
            logger.warn( "Connecting to Cassandra failed, reason: {}", e.toString() );
        }
    }

    public Session getSession()
    {
        return session;
    }
}
