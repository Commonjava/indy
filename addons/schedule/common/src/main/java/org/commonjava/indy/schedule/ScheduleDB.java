package org.commonjava.indy.schedule;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.commonjava.indy.schedule.conf.ScheduleDBConfig;
import org.commonjava.indy.schedule.datastax.model.DtxExpiration;
import org.commonjava.indy.schedule.datastax.model.DtxSchedule;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@ApplicationScoped
public class ScheduleDB
{

    @Inject
    CassandraClient client;

    @Inject
    ScheduleDBConfig config;

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private Session session;

    private Mapper<DtxSchedule> scheduleMapper;

    private Mapper<DtxExpiration> expirationMapper;

    private PreparedStatement preparedExpiredQuery;

    private PreparedStatement preparedSingleScheduleQuery;

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
        session.execute( ScheduleDBUtil.getSchemaCreateTableExpiration( keyspace ) );

        MappingManager manager = new MappingManager( session );

        scheduleMapper = manager.mapper( DtxSchedule.class, keyspace );
        expirationMapper = manager.mapper( DtxExpiration.class, keyspace );

        preparedExpiredQuery = session.prepare( "SELECT scheduleuid, expirationtime, storekey, jobname FROM " + keyspace + "." + ScheduleDBUtil.TABLE_EXPIRATION + " WHERE expirationpid = ?" );

        preparedSingleScheduleQuery = session.prepare( "SELECT storekey, jobtype, jobname, scheduletime, lifespan, expired FROM " + keyspace + "." + ScheduleDBUtil.TABLE_SCHEDULE + " WHERE storekey = ? and  jobname = ?" );

        preparedExpiredUpdate = session.prepare( "UPDATE " + keyspace + "." + ScheduleDBUtil.TABLE_SCHEDULE + " SET expired = true WHERE  storekey = ? and  jobname = ?"  );

    }

    public void createSchedule( String storeKey, String jobType, String jobName, Long timeout )
    {

        UUID scheduleUID = UUID.randomUUID();
        Date scheduleTime = new Date();

        DtxSchedule schedule = new DtxSchedule( storeKey, jobType, jobName, scheduleTime, timeout );
        schedule.setExpired( Boolean.FALSE );
        schedule.setScheduleUID( scheduleUID );
        scheduleMapper.save( schedule );

        DtxExpiration expiration = new DtxExpiration();
        Date expirationTime = calculateExpirationTime( scheduleTime, timeout );
        expiration.setExpirationPID( calculateExpirationPID( expirationTime ) );
        expiration.setExpirationTime( expirationTime );
        expiration.setScheduleUID( scheduleUID );
        expiration.setJobName( jobName );
        expiration.setStorekey( storeKey );
        expirationMapper.save( expiration );

    }

    private Long calculateExpirationPID( Date date )
    {
        return date.getTime() / config.getPartitionKeyRange();
    }

    private Date calculateExpirationTime( Date scheduleTime, Long timeout)
    {
        return new Date( scheduleTime.getTime() + 1000 * timeout );
    }

    public DtxSchedule querySchedule( String storeKey, String jobName )
    {
        BoundStatement bound = preparedSingleScheduleQuery.bind( storeKey, jobName );
        ResultSet resultSet = session.execute( bound );

        Row row = resultSet.one();

        if ( row != null )
        {
            DtxSchedule schedule = new DtxSchedule(  );
            schedule.setStoreKey( row.getString( "storekey" ) );
            schedule.setJobType( row.getString( "jobtype" ) );
            schedule.setJobName( row.getString( "jobname" ) );
            schedule.setExpired( row.getBool( "expired" ) );
            schedule.setScheduleTime( row.getTimestamp( "scheduletime" ) );
            schedule.setLifespan( row.getLong( "lifespan" ) );

            return schedule;
        }

        return null;
    }

    public Collection<DtxExpiration> queryExpirations( Date date )
    {

        Long pid = calculateExpirationPID( date );

        Collection<DtxExpiration> expirations = new ArrayList<>(  );

        BoundStatement bound = preparedExpiredQuery.bind( pid );
        ResultSet resultSet = session.execute( bound );
        resultSet.forEach( row -> {
            DtxExpiration expiration = new DtxExpiration();
            expiration.setExpirationTime( row.getTimestamp( "expirationtime" ) );
            expiration.setStorekey( row.getString( "storekey" ) );
            expiration.setJobName( row.getString( "jobname" ) );
            expirations.add( expiration );
        } );

        return expirations;
    }

    public void queryAndSetExpiredSchedule( Date date )
    {
        Collection<DtxExpiration> expirations = queryExpirations( date );
        expirations.forEach( expiration -> {
            if ( expiration.getExpirationTime().before( new Date() ) )
            {
                DtxSchedule schedule = querySchedule( expiration.getStorekey(), expiration.getJobName() );

                Date calExpirationTime = calculateExpirationTime( schedule.getScheduleTime(), schedule.getLifespan() );
                if ( schedule != null && !schedule.getExpired() &&
                                !calExpirationTime.after( expiration.getExpirationTime() ) )
                {
                    BoundStatement boundU = preparedExpiredUpdate.bind( schedule.getStoreKey(), schedule.getJobName() );
                    session.execute( boundU );

                    logger.info( "Expired entry: {}", schedule );
                }
            }
        } );
    }

}
