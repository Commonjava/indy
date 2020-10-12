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
