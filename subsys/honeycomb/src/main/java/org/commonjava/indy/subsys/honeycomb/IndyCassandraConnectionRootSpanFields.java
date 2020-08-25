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
    private CassandraClient cassandraClient;

    @Inject
    public IndyCassandraConnectionRootSpanFields( CassandraClient cassandraClient )
    {
        this.cassandraClient = cassandraClient;
    }

    @Override
    protected Map<String, Session> getSessions()
    {
        return Collections.unmodifiableMap( cassandraClient.getSessions() );
    }
}
