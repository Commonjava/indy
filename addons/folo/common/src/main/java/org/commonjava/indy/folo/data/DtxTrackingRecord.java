/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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


import com.datastax.driver.core.Row;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.commonjava.indy.folo.data.FoloRecordCassandra.TABLE_NAME;

@Table(name = TABLE_NAME )
public class DtxTrackingRecord {

    private final static Boolean SEALED = true;
    private final static Boolean IN_PROGRESS = false;

    @PartitionKey
    @Column(name = "tracking_key")
    String trackingKey;

    @Column(name = "sealed")
    Boolean state;

    @ClusteringColumn(0)
    @Column(name = "store_key")
    String storeKey;

    @Column(name = "access_channel")
    String  accessChannel;

    @ClusteringColumn(1)
    @Column(name = "path")
    String  path;

    @Column(name = "origin_url")
    String  originUrl;

    @Column(name = "local_url")
    String localUrl;

    @ClusteringColumn(2)
    @Column(name = "store_effect")
    String storeEffect;

    @Column(name = "md5")
    String  md5;

    @Column(name = "sha256")
    String sha256;

    @Column(name = "sha1")
    String sha1;

    @Column(name = "size")
    Long size;

    @Column(name = "started") // started timestamp *
    Long started;

    @Column(name = "timestamps")
    Set<Long> timestamps;


    public DtxTrackingRecord() {
    }

    public DtxTrackingRecord(String trackingKey, Boolean state, String storeKey, String accessChannel,
                             String path, String originUrl, String localUrl, String storeEffect, String md5,
                             String sha256, String sha1, Long size, Long started ,Set<Long> timestamps) {
        this.trackingKey = trackingKey;
        this.state = state;
        this.storeKey = storeKey;
        this.accessChannel = accessChannel;
        this.path = path;
        this.originUrl = originUrl;
        this.localUrl = localUrl;
        this.storeEffect = storeEffect;
        this.md5 = md5;
        this.sha256 = sha256;
        this.sha1 = sha1;
        this.size = size;
        this.started =  started;  // started timestamp *
        this.timestamps = timestamps;
    }

    public DtxTrackingRecord(TrackedContentEntry entry) {
        this.trackingKey = entry.getTrackingKey().getId();
        this.state  = IN_PROGRESS;
        this.storeKey = entry.getStoreKey().toString();
        this.accessChannel = entry.getAccessChannel().toString();
        this.path = entry.getPath();
        this.originUrl = entry.getOriginUrl() == null  || entry.getOriginUrl().isEmpty() ? "" : entry.getOriginUrl();
        this.localUrl = "";
        this.storeEffect = entry.getEffect().toString();
        this.md5 = entry.getMd5();
        this.sha256 = entry.getSha256();
        this.sha1 = entry.getSha1();
        this.size = entry.getSize();
        this.started = System.currentTimeMillis();  // started timestamp *
        this.timestamps =
                ((entry.getTimestamps()==null) || entry.getTimestamps().isEmpty())
                        ? new HashSet<>() : entry.getTimestamps();
    }

    public static DtxTrackingRecord fromTrackedContentEntry(TrackedContentEntry entry,Boolean sealed) {
        if(entry == null) {
            return new DtxTrackingRecord();  // TODO  check for  null value
        }
        boolean sealedRecord = sealed ? SEALED : IN_PROGRESS ;
        DtxTrackingRecord dtxTrackingRecord = new DtxTrackingRecord();
        dtxTrackingRecord.setTrackingKey(entry.getTrackingKey().getId());
        dtxTrackingRecord.setState(sealedRecord);
        dtxTrackingRecord.setAccessChannel(entry.getAccessChannel().toString());
        dtxTrackingRecord.setMd5(entry.getMd5());
        dtxTrackingRecord.setSha1(entry.getSha1());
        dtxTrackingRecord.setSha256(entry.getSha256());
        dtxTrackingRecord.setOriginUrl(entry.getOriginUrl()==null ? "" : entry.getOriginUrl());
        dtxTrackingRecord.setLocalUrl("");  // TODO  localURL from ttrackingcontententry???
        dtxTrackingRecord.setPath(entry.getPath());
        dtxTrackingRecord.setSize(entry.getSize());
        dtxTrackingRecord.setStoreEffect(entry.getEffect().toString());
        dtxTrackingRecord.setStarted(System.currentTimeMillis()); // started timestamp *
        dtxTrackingRecord.setTimestamps(entry.getTimestamps()==null ? new HashSet<>() : entry.getTimestamps());
        dtxTrackingRecord.setStoreKey(entry.getStoreKey().toString());
        return dtxTrackingRecord;
    }

    public static TrackedContentEntry toTrackingContentEntry(DtxTrackingRecord record) {

        TrackedContentEntry trackedContentEntry = new TrackedContentEntry();

        trackedContentEntry.setTrackingKey(new TrackingKey(record.getTrackingKey()));
        if(record.getAccessChannel() == null || record.getAccessChannel().isEmpty()) {
            trackedContentEntry.setAccessChannel(AccessChannel.NATIVE);
        } else {
            trackedContentEntry.setAccessChannel(AccessChannel.valueOf(record.getAccessChannel()));
        }
        trackedContentEntry.setEffect(StoreEffect.valueOf(record.getStoreEffect()));
        trackedContentEntry.setMd5(record.getMd5());
        trackedContentEntry.setSha1(record.getSha1());
        trackedContentEntry.setSha256(record.getSha256());
        trackedContentEntry.setSize(record.getSize());
        trackedContentEntry.setStoreKey(StoreKey.fromString(record.getStoreKey()));
        trackedContentEntry.setOriginUrl(record.getOriginUrl());
        trackedContentEntry.setPath(record.getPath());
        trackedContentEntry.setTimestamps(record.getTimestamps());

        return trackedContentEntry;
    }

    public static DtxTrackingRecord fromCassandraRow(Row row) {

        DtxTrackingRecord dtxTrackingRecord = new DtxTrackingRecord();
        dtxTrackingRecord.setTrackingKey(row.getString("tracking_key"));
        dtxTrackingRecord.setState(row.getBool("sealed"));
        dtxTrackingRecord.setLocalUrl(row.getString("local_url"));
        dtxTrackingRecord.setOriginUrl(row.getString("origin_url"));
        dtxTrackingRecord.setTimestamps(row.getSet("timestamps",Long.class));
        dtxTrackingRecord.setPath(row.getString("path"));
        dtxTrackingRecord.setStoreEffect(row.getString("store_effect"));
        dtxTrackingRecord.setSha256(row.getString("sha256"));
        dtxTrackingRecord.setSha1(row.getString("sha1"));
        dtxTrackingRecord.setMd5(row.getString("md5"));
        dtxTrackingRecord.setSize(row.getLong("size"));
        dtxTrackingRecord.setStoreKey(row.getString("store_key"));
        dtxTrackingRecord.setAccessChannel(row.getString("access_channel"));
        dtxTrackingRecord.setStarted(row.getLong("started")); // started timestamp *

        return dtxTrackingRecord;
    }

    public String getLocalUrl() {
        return localUrl;
    }

    public void setLocalUrl(String localUrl) {
        this.localUrl = localUrl;
    }

    public Boolean getState() {
        return state;
    }

    public void setState(Boolean state) {
        this.state = state;
    }

    public String getTrackingKey() {
        return trackingKey;
    }

    public void setTrackingKey(String trackingKey) {
        this.trackingKey = trackingKey;
    }

    public String getStoreKey() {
        return storeKey;
    }

    public void setStoreKey(String storeKey) {
        this.storeKey = storeKey;
    }

    public String getAccessChannel() {
        return accessChannel;
    }

    public void setAccessChannel(String accessChannel) {
        this.accessChannel = accessChannel;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getStoreEffect() {
        return storeEffect;
    }

    public void setStoreEffect(String storeEffect) {
        this.storeEffect = storeEffect;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getStarted() {
        return started;
    }

    public void setStarted(Long started) {
        this.started = started;
    }

    public Set<Long> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(Set<Long> timestamps) {
        this.timestamps = timestamps;
    }
}
