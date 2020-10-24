package org.commonjava.indy.folo.data;

import com.datastax.driver.core.*;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.folo.change.FoloBackupListener;
import org.commonjava.indy.folo.change.FoloExpirationWarningListener;
import org.commonjava.indy.folo.conf.FoloConfig;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.commonjava.indy.folo.data.DtxTrackingRecord.fromCassandraRow;

@ApplicationScoped
@FoloStoreToCassandra
public class FoloRecordCassandra implements FoloRecord,StartupAction {

    private final static String DOWNLOADS = "DOWNLOAD";
    private final static String UPLOADS = "UPLOAD";

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    CassandraClient cassandraClient;

    @Inject
    FoloConfig config;

    @Context
    UriInfo uriInfo;

    @FoloInprogressCache
    @Inject
    private CacheHandle<TrackedContentEntry, TrackedContentEntry> inProgressRecords;

    @FoloSealedCache
    @Inject
    private CacheHandle<TrackingKey, TrackedContent> sealedRecords;

    @Inject
    private FoloBackupListener foloBackupListener;

    @Inject
    private FoloExpirationWarningListener expirationWarningListener;



    private Session session;
    private Mapper<DtxTrackingRecord> trackingMapper;

    private PreparedStatement getTrackingRecordByBuildIdAndPath;
    private PreparedStatement getTrackingRecordBySealed;
    private PreparedStatement getTrackingRecordsByTrackingKey;


    private static String createFoloRecordsTable( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + ".records ("
                + "tracking_key text,"
                + "sealed boolean,"
                + "store_key text,"
                + "access_channel text,"
                + "path text,"
                + "origin_url text,"
                + "local_url text,"
                + "store_effect text,"
                + "md5 text,"
                + "sha256 text,"
                + "sha1 text,"
                + "size bigint,"
                + "timestamps set<bigint>,"
                + "PRIMARY KEY ((tracking_key),path)"
                + ");";
    }

    private static String createFoloKeyspace( String keyspace)
    {
        return "CREATE KEYSPACE IF NOT EXISTS " + keyspace
                + " WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1 };";
    }


    private static String createFoloSealedIdx(String  keyspace) {
        return "CREATE INDEX IF NOT EXISTS sealed_idx ON " + keyspace + ".records (sealed);";
    }



    @PostConstruct
    public void initialize() {

        logger.warn("-- Creating Cassandra Folo Records Keyspace and Tables  ---");

        String foloCassandraKeyspace = config.getFoloCassandraKeyspace();

        session = cassandraClient.getSession(foloCassandraKeyspace);
        session.execute(createFoloKeyspace(foloCassandraKeyspace));
        session.execute(createFoloRecordsTable(foloCassandraKeyspace));
        session.execute(createFoloSealedIdx(foloCassandraKeyspace));

        MappingManager mappingManager = new MappingManager(session);
        trackingMapper = mappingManager.mapper(DtxTrackingRecord.class,foloCassandraKeyspace);

        getTrackingRecordByBuildIdAndPath =
                session.prepare("SELECT * FROM " + foloCassandraKeyspace + ".records WHERE tracking_key=? AND path=?;");
        getTrackingRecordBySealed =
                session.prepare("SELECT * FROM " +  foloCassandraKeyspace + ".records WHERE sealed=?;");

        getTrackingRecordsByTrackingKey =
                session.prepare("SELECT * FROM "  + foloCassandraKeyspace + ".records WHERE tracking_key=?;");

        logger.warn("-- Cassandra Folo Records Keyspace and Tables created  ---");

    }

    @Override
    public boolean recordArtifact(TrackedContentEntry entry) throws FoloContentException, IndyWorkflowException {

        String buildId = entry.getTrackingKey().getId();
        String path = entry.getPath();

        BoundStatement bind = getTrackingRecordByBuildIdAndPath.bind(buildId,path);
        ResultSet trackingRecord = session.execute(bind);
        Row one = trackingRecord.one();

        if(one!=null) {

            DtxTrackingRecord dtxTrackingRecord = fromCassandraRow(one);

            Boolean state = dtxTrackingRecord.getState();

            if(state) {
                throw new FoloContentException( "Tracking record: {} is already sealed!", entry.getTrackingKey() );
            }  else {
                DtxTrackingRecord dtxTrackingRecord1 =
                        DtxTrackingRecord.fromTrackedContentEntry(entry,false);

                if(dtxTrackingRecord.getTrackingKey().equals(dtxTrackingRecord1.getTrackingKey()) &&
                    dtxTrackingRecord.getPath().equals(dtxTrackingRecord1.getPath())) {
                    trackingMapper.save(dtxTrackingRecord1);
                    return true;
                } else {
                    return false;
                }
            }

        } else {

            DtxTrackingRecord dtxTrackingRecord = new DtxTrackingRecord(entry);
            trackingMapper.save(dtxTrackingRecord); //  optional Options with TTL, timestamp...
            return true;
        }


    }

    @Override
    public void delete(TrackingKey key) {

        // Without delete logic because we need to keep all records in cassandra for auditing

        // get  records from DB
//        BoundStatement bind = getTrackingRecordsByTrackingKey.bind(key.getId());
//        ResultSet trackingRecord = session.execute(bind);
//        List<Row> all = trackingRecord.all();

        // transform from row to  dtxTrackingRecord
//        List<DtxTrackingRecord> records =  new ArrayList<>();
//        for (Row row :  all) {
//            DtxTrackingRecord record = fromCassandraRow(row);
//            records.add(record);
//        }

        // check if they are  temporary builds  maven:hosted:temporary-builds
//        boolean b =
//                records.stream()
//                    .map(dtxRec -> StoreKey.fromString(dtxRec.getStoreKey()))
//                    .allMatch(storeKey -> storeKey.getType().singularEndpointName().equalsIgnoreCase(TEMP_BUILDS));

        // if they  are delete them
//        if(b) {
//            for (DtxTrackingRecord record : records) {
//                trackingMapper.deleteAsync(record);
//            }
//        }
    }

    @Override
    public void replaceTrackingRecord(TrackedContent record) {

        saveTrackedContentRecords(record);

    }

    @Override
    public boolean hasRecord(TrackingKey key) {
        return hasSealedRecord(key) || hasInProgressRecord(key);
    }

    public boolean hasSealedRecord(TrackingKey key) {
        BoundStatement bind = getTrackingRecordsByTrackingKey.bind(key);
        ResultSet execute = session.execute(bind);
        Row one = execute.one();
        if(one != null) {
            Boolean sealed = one.getBool("sealed");
            if(sealed) {
                return true;
            } else {
                return false;
            }
        }else {
            return false;
        }
    }

    public boolean hasInProgressRecord(TrackingKey key) {
        BoundStatement bind = getTrackingRecordsByTrackingKey.bind(key);
        ResultSet execute = session.execute(bind);
        Row one = execute.one();
        if(one != null) {
            Boolean sealed = one.getBool("sealed");
            if(!sealed) {
                return true;
            } else {
                return false;
            }
        }else {
            return false;
        }
    }

    @Override
    public TrackedContent get(TrackingKey key) {
        List<DtxTrackingRecord> trackingRecords =  getDtxTrackingRecordsFromDb(key);
        return transformDtxTrackingRecordToTrackingContent(key,trackingRecords);
    }

    @Override
    public TrackedContent seal(TrackingKey trackingKey) {
        List<DtxTrackingRecord> trackingRecords =  getDtxTrackingRecordsFromDb(trackingKey);

        if(trackingRecords == null ||  trackingRecords.isEmpty()) {
            logger.debug( "Tracking record: {} doesn't exist! Returning empty record.", trackingKey );
            return new TrackedContent(trackingKey,new HashSet<>() ,  new HashSet<>());
        }

        DtxTrackingRecord recordCheck = trackingRecords.get(0);
        if(recordCheck.getState()) {
            logger.debug( "Tracking record: {} already sealed! Returning sealed record.", trackingKey );
            return transformDtxTrackingRecordToTrackingContent(trackingKey,trackingRecords);
        }
        logger.debug( "Sealing record for: {}", trackingKey );
        for(DtxTrackingRecord record : trackingRecords) {
            record.setState(true);
            trackingMapper.save(record);
        }
        return transformDtxTrackingRecordToTrackingContent(trackingKey,trackingRecords);
    }

    @Override
    public Set<TrackingKey> getInProgressTrackingKey() {
        return getTrackingKeys(false);
    }

    @Override
    public Set<TrackingKey> getSealedTrackingKey() {
        return getTrackingKeys(true);
    }




    @Override
    public Set<TrackedContent> getSealed() {

        Set<TrackedContent> trackedContents = new HashSet<>();
        Set<TrackingKey> sealedTrackingKeys = getSealedTrackingKey();

        for(TrackingKey trackingKey : sealedTrackingKeys) {

            List<DtxTrackingRecord> dtxTrackingRecordsFromDb = getDtxTrackingRecordsFromDb(trackingKey);
            TrackedContent trackedContent = transformDtxTrackingRecordToTrackingContent(trackingKey, dtxTrackingRecordsFromDb);

            trackedContents.add(trackedContent);
        }

        return trackedContents;
    }

    @Override
    public void addSealedRecord(TrackedContent record) {

        saveTrackedContentRecords(record);

    }

    @Override
    public void start() throws IndyLifecycleException {
        logger.warn("--- FoloRecordsCassandra starting up ---");
    }

    @Override
    public int getStartupPriority() {
        return 0;
    }

    @Override
    public String getId() {
        return "Folo2Cassandra";
    }

    private TrackedContent transformDtxTrackingRecordToTrackingContent(TrackingKey trackingKey, List<DtxTrackingRecord> trackingRecords) {

        List<TrackedContentEntry> records =  new ArrayList<>();

        for(DtxTrackingRecord record : trackingRecords) {
            records.add(DtxTrackingRecord.toTrackingContentEntry(record));
        }
        Set<TrackedContentEntry> uploads =
                records.stream().filter(record -> record.getEffect().toString().equals(UPLOADS)).collect(Collectors.toSet());

//        logger.warn("-- Processing {} uploads  from  tracking key {} " ,  uploads.size() ,  trackingKey);

        Set<TrackedContentEntry> downloads =
                records.stream().filter(record -> record.getEffect().toString().equals(DOWNLOADS)).collect(Collectors.toSet());

//        logger.warn("-- Processing {} downloads  from  tracking key {} " ,  downloads.size() ,  trackingKey);

        TrackedContent trackedContent =
                new TrackedContent(trackingKey, uploads, downloads);

        return trackedContent;

    }

    private List<DtxTrackingRecord>  getDtxTrackingRecordsFromDb(TrackingKey trackingKey)  {

        List<DtxTrackingRecord> trackingRecords =  new ArrayList<>();

        BoundStatement bind = getTrackingRecordsByTrackingKey.bind(trackingKey.getId());
        ResultSet execute = session.execute(bind);
        List<Row> allTrackingRecordsByTrackingKey = execute.all();

//        logger.warn("-- Fetched {} tracking  records from key {}",allTrackingRecordsByTrackingKey.size(),trackingKey);

        Iterator<Row> iteratorDtxTrackingRecords = allTrackingRecordsByTrackingKey.iterator();
        while (iteratorDtxTrackingRecords.hasNext()) {
            Row next = iteratorDtxTrackingRecords.next();
            DtxTrackingRecord dtxTrackingRecord = new DtxTrackingRecord();
            dtxTrackingRecord.setTrackingKey(next.getString("tracking_key"));
            dtxTrackingRecord.setState(next.getBool("sealed"));
            dtxTrackingRecord.setLocalUrl(next.getString("local_url"));
            dtxTrackingRecord.setOriginUrl(next.getString("origin_url"));
            dtxTrackingRecord.setTimestamps(next.getSet("timestamps",Long.class));
            dtxTrackingRecord.setPath(next.getString("path"));
            dtxTrackingRecord.setStoreEffect(next.getString("store_effect"));
            dtxTrackingRecord.setSha256(next.getString("sha256"));
            dtxTrackingRecord.setSha1(next.getString("sha1"));
            dtxTrackingRecord.setMd5(next.getString("md5"));
            dtxTrackingRecord.setSize(next.getLong("size"));
            dtxTrackingRecord.setStoreKey(next.getString("store_key"));
            dtxTrackingRecord.setAccessChannel(next.getString("access_channel"));
            trackingRecords.add(dtxTrackingRecord);
        }
        return trackingRecords;
    }

    private void saveTrackedContentRecords(TrackedContent record) {
        Set<TrackedContentEntry> downloads = record.getDownloads();
        Set<TrackedContentEntry> uploads = record.getUploads();
        TrackingKey key = record.getKey();

        for(TrackedContentEntry downloadEntry : downloads) {
            DtxTrackingRecord downloadRecord =
                    DtxTrackingRecord.fromTrackedContentEntry(downloadEntry, true);
            trackingMapper.save(downloadRecord);
        }

        for(TrackedContentEntry uploadEntry : uploads) {
            DtxTrackingRecord uploadRecord =
                    DtxTrackingRecord.fromTrackedContentEntry(uploadEntry, true);
            trackingMapper.save(uploadRecord);
        }
    }

    private Set<TrackingKey> getTrackingKeys(Boolean sealed) {
        BoundStatement inProgress = getTrackingRecordBySealed.bind(sealed);
        ResultSet getInProgressRecords = session.execute(inProgress);
        List<Row> allInProgress = getInProgressRecords.all();
        Iterator<Row> iterator = allInProgress.iterator();

        Set<TrackingKey> trackingKeys = new HashSet<>();
        while (iterator.hasNext()) {
            Row next = iterator.next();
            String tracking_key = next.getString("tracking_key");
            trackingKeys.add(new TrackingKey(tracking_key));
        }
        //**/
        return trackingKeys.stream().distinct().collect(Collectors.toSet());
    }
}
