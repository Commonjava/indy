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
package org.commonjava.indy.subsys.honeycomb;

import com.datastax.driver.core.Session;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.o11yphant.honeycomb.impl.CassandraConnectionRootSpanFields;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

@ApplicationScoped
public class IndyCassandraConnectionRootSpanFields
                extends CassandraConnectionRootSpanFields
{

    private final Map<String, Session> sessions;

    @Inject
    public IndyCassandraConnectionRootSpanFields( CassandraClient cassandraClient )
    {
        this.sessions = Collections.unmodifiableMap( cassandraClient.getSessions() );
    }

    @Override
    protected Map<String, Session> getSessions()
    {
        return sessions;
    }
}
