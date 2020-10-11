package org.commonjava.indy.folo.data;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Set;

@Table(name = "records" )
public class DtxTrackingRecord {

    private final static Boolean SEALED = true;
    private final static Boolean IN_PROGRESS = false;


    @PartitionKey(0)
    @Column(name = "tracking_key")
    String trackingKey;

    @Column(name = "sealed")
    Boolean state;

    @Column(name = "store_key")
    String storeKey;

    @Column(name = "access_channel")
    String  accessChannel;

    @PartitionKey(1)
    @Column(name = "path")
    String  path;

    @Column(name = "origin_url")
    String  originUrl;

    @Column(name = "local_url")
    String localUrl;

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

    @Column(name = "timestamps")
    Set<Long> timestamps;


    public DtxTrackingRecord() {
    }

    public DtxTrackingRecord(String trackingKey, Boolean state, String storeKey, String accessChannel,
                             String path, String originUrl, String localUrl, String storeEffect, String md5,
                             String sha256, String sha1, Long size, Set<Long> timestamps) {
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
        this.timestamps = timestamps;
    }

    public DtxTrackingRecord(TrackedContentEntry entry) {
        this.trackingKey = entry.getTrackingKey().getId();
        this.state  = IN_PROGRESS;
        this.storeKey = entry.getStoreKey().toString();
        this.accessChannel = entry.getAccessChannel().toString();
        this.path = entry.getPath();
        this.originUrl = entry.getOriginUrl();
        this.localUrl = "";
        this.storeEffect = entry.getEffect().toString();
        this.md5 = entry.getMd5();
        this.sha256 = entry.getSha256();
        this.sha1 = entry.getSha1();
        this.size = entry.getSize();
        this.timestamps = entry.getTimestamps();
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
        dtxTrackingRecord.setTimestamps(entry.getTimestamps());
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

    public Set<Long> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(Set<Long> timestamps) {
        this.timestamps = timestamps;
    }
}
