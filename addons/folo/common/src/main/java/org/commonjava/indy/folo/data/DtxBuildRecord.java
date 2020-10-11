package org.commonjava.indy.folo.data;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Date;

@Table(name = "builds" ,  readConsistency = "QUORUM" , writeConsistency = "QUORUM")
public class DtxBuildRecord {


    @PartitionKey
    private String id;

    @Column(name = "sealed")
    private Boolean sealed;

    @Column(name = "started")
    private Date started;

    @Column(name = "finished")
    private Date finished;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getFinished() {
        return finished;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    public Boolean getSealed() {
        return sealed;
    }

    public void setSealed(Boolean sealed) {
        this.sealed = sealed;
    }

}
