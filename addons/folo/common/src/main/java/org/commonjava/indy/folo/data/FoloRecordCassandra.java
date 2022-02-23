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
package org.commonjava.indy.folo.data;

import com.datastax.driver.core.*;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.folo.conf.FoloConfig;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.util.SchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
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

    @Inject
    IndyConfiguration indyConfig;

    @Context
    UriInfo uriInfo;

    private Session session;
    private Mapper<DtxTrackingRecord> trackingMapper;

    private PreparedStatement getTrackingRecord;
    private PreparedStatement getTrackingKeys;
    private PreparedStatement getTrackingRecordsByTrackingKey;
    private PreparedStatement isTrackingRecordExist;

    static final String TABLE_NAME = "records2"; // Change from records to records2 due to primary key change

    private static String createFoloRecordsTable( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_NAME + " ("
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
                + "started bigint," // started timestamp *
                + "timestamps set<bigint>,"
                + "PRIMARY KEY ((tracking_key),store_key,path,store_effect)"
                + ");";
    }

    @PostConstruct
    public void initialize() {

        logger.info("-- Creating Cassandra Folo Records Keyspace and Tables");

        String foloCassandraKeyspace = config.getFoloCassandraKeyspace();

        session = cassandraClient.getSession(foloCassandraKeyspace);
        session.execute( SchemaUtils.getSchemaCreateKeyspace( foloCassandraKeyspace, indyConfig.getKeyspaceReplicas() ));
        session.execute(createFoloRecordsTable(foloCassandraKeyspace));

        MappingManager mappingManager = new MappingManager(session);
        trackingMapper = mappingManager.mapper(DtxTrackingRecord.class,foloCassandraKeyspace);

        getTrackingRecord =
                session.prepare("SELECT * FROM " + foloCassandraKeyspace + "." + TABLE_NAME + " WHERE tracking_key=? AND store_key=? AND path=? AND store_effect=?;");
        getTrackingKeys =
                session.prepare("SELECT distinct tracking_key FROM " +  foloCassandraKeyspace + "." + TABLE_NAME + ";");

        getTrackingRecordsByTrackingKey =
                session.prepare("SELECT * FROM "  + foloCassandraKeyspace + "." + TABLE_NAME + " WHERE tracking_key=?;");

        isTrackingRecordExist =
                session.prepare("SELECT count(*) FROM "  + foloCassandraKeyspace + "." + TABLE_NAME + " WHERE tracking_key=?;");

        logger.info("-- Cassandra Folo Records Keyspace and Tables created");
    }

    @Override
    public boolean recordArtifact(TrackedContentEntry entry) throws FoloContentException, IndyWorkflowException {

        String buildId = entry.getTrackingKey().getId();
        String storeKey = entry.getStoreKey().toString();
        String path = entry.getPath();
        String effect = entry.getEffect().toString();

        BoundStatement bind = getTrackingRecord.bind( buildId, storeKey, path, effect );
        ResultSet trackingRecord = session.execute(bind);
        Row one = trackingRecord.one();

        if(one!=null) {
            DtxTrackingRecord dtxTrackingRecord = fromCassandraRow(one);
            Boolean state = dtxTrackingRecord.getState();
            if(state) {
                throw new FoloContentException( "Tracking record: {} is already sealed!", entry.getTrackingKey() );
            }
        } else {
            DtxTrackingRecord dtxTrackingRecord = new DtxTrackingRecord(entry);
            trackingMapper.save(dtxTrackingRecord); //  optional Options with TTL, timestamp...
        }
        return true;
    }

    @Override
    public void delete(TrackingKey key) {
        // We should not need delete logic because we need to keep all records in cassandra for auditing. But instead of
        // throwing an exception, we only need an error message here to avoid following error.

        //        throw new UnsupportedOperationException( "Deleting tracking record is not supported by Cassandra Folo" );
        logger.warn( "Deleting tracking record is not supported by Cassandra Folo" );
    }

    @Override
    public void replaceTrackingRecord(TrackedContent record) {
        saveTrackedContentRecords(record);
    }

    @Override
    public boolean hasRecord(TrackingKey key) {
        BoundStatement bind = isTrackingRecordExist.bind(key);
        ResultSet result = session.execute(bind);
        Row row = result.one();
        boolean exists = false;
        if ( row != null )
        {
            long count = row.get( 0, Long.class );
            exists = count > 0;
        }
        logger.trace( "{} {}", key, ( exists ? "exists" : "not exists" ) );
        return exists;
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
        throw new UnsupportedOperationException( "Getting in-progress tracking keys are not supported by Cassandra Folo" );
    }

    @Override
    public Set<TrackingKey> getSealedTrackingKey() {
        return getTrackingKeys();
    }

    // This may fail given a huge dataset (oom). Only used for test purpose !
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
        logger.info("--- FoloRecordsCassandra starting up");
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

    private Set<TrackingKey> getTrackingKeys() {
        BoundStatement statement = getTrackingKeys.bind();
        ResultSet resultSet = session.execute(statement);
        List<Row> all = resultSet.all();
        Iterator<Row> iterator = all.iterator();

        Set<TrackingKey> trackingKeys = new HashSet<>();
        while (iterator.hasNext()) {
            Row next = iterator.next();
            String tracking_key = next.getString("tracking_key");
            trackingKeys.add(new TrackingKey(tracking_key));
        }
        return trackingKeys.stream().collect(Collectors.toSet());
    }
}
