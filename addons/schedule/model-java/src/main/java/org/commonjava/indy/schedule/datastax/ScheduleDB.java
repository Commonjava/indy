package org.commonjava.indy.schedule.datastax;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.commonjava.indy.schedule.datastax.model.DtxSchedule;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;

@ApplicationScoped
public class ScheduleDB
{

    @Inject
    CassandraClient client;

    @Inject
    ScheduleDBConfig config;

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private Session session;

    private Mapper<DtxSchedule> expirationMapper;

    private PreparedStatement preparedTTLExpiredQuery;

    private PreparedStatement preparedExpiredQuery;

    private PreparedStatement preparedTTLSetting;

    private PreparedStatement preparedExpiredUpdate;

    public ScheduleDB( ScheduleDBConfig config, CassandraClient client )
    {
        this.config = config;
        this.client = client;
        init();
    }

    @PostConstruct
    public void init()
    {

        String keyspace = config.getScheduleKeyspace();

        session = client.getSession( keyspace );

        session.execute( ScheduleDBUtil.getSchemaCreateKeyspace( config, keyspace ) );
        session.execute( ScheduleDBUtil.getSchemaCreateTableSchedule( keyspace ) );
        session.execute( ScheduleDBUtil.getSchemaCreateTypeIndex4Schedule( keyspace ) );

        MappingManager manager = new MappingManager( session );

        expirationMapper = manager.mapper( DtxSchedule.class, keyspace );

        preparedTTLExpiredQuery = session.prepare( "SELECT storekey, jobtype, jobname, scheduletime, lifespan, expired, ttl(ttl) as ttl FROM " + keyspace + "." + ScheduleDBUtil.TABLE_SCHEDULE + " WHERE expired = false ALLOW FILTERING" );

        preparedExpiredQuery = session.prepare( "SELECT storekey, jobtype, jobname, scheduletime, lifespan, expired FROM " + keyspace + "." + ScheduleDBUtil.TABLE_SCHEDULE + " WHERE expired = true ALLOW FILTERING" );

        preparedTTLSetting = session.prepare( "UPDATE " + keyspace +  "." + ScheduleDBUtil.TABLE_SCHEDULE + " USING TTL ? SET ttl=? WHERE storekey = ? and  jobname = ?" );

        preparedExpiredUpdate = session.prepare( "UPDATE " + keyspace + "." + ScheduleDBUtil.TABLE_SCHEDULE + " SET expired = true WHERE  storekey = ? and  jobname = ?"  );
    }



    public void createSchedule( String storeKey, String jobType, String jobName, Long timeout )
    {

        DtxSchedule schedule = new DtxSchedule( storeKey, jobType, jobName, new Date(), timeout );
        schedule.setExpired( Boolean.FALSE );

        expirationMapper.save( schedule );

        BoundStatement bound = preparedTTLSetting.bind( schedule.getLifespan().intValue(), schedule.getLifespan(), schedule.getStoreKey(), schedule.getJobName() );
        session.execute( bound );
    }

    public Collection<DtxSchedule> queryExpiredSchedule()
    {
        BoundStatement bound = preparedExpiredQuery.bind();
        ResultSet resultSet = session.execute( bound );

        List<DtxSchedule> schedules = new ArrayList<>(  );

        resultSet.forEach( row -> {
            if ( row != null && row.getBool( "expired" ) )
            {
                DtxSchedule schedule = new DtxSchedule(  );
                schedule.setJobType( row.getString( "jobType" ) );
                schedule.setJobName( row.getString( "jobName" ) );
                schedule.setExpired( row.getBool( "expired" ) );

                schedules.add( schedule );
            }
        } );

        return schedules;
    }

    public void queryTTLAndSetExpiredSchedule()
    {
        BoundStatement bound = preparedTTLExpiredQuery.bind();
        ResultSet resultSet = session.execute( bound );
        resultSet.forEach( row -> {
            if ( row != null && row.getInt( "ttl" ) == 0 )
            {
                BoundStatement boundU =
                                preparedExpiredUpdate.bind( row.getString( "storekey" ), row.getString( "jobname" ) );
                session.execute( boundU );

                logger.info( "Expired entry: {}", row );
            }
        } );
    }

}
