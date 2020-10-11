package org.commonjava.indy.folo.data;


import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Param;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface FoloAccessor {

    @Query("SELECT * FROM folo.records WHERE path=?")
    Result<DtxTrackingRecord> getTrackingRecordPath(@Param String path);

    @Query("SELECT * FROM folo.records WHERE sealed=?")
    Result<DtxTrackingRecord> getTrackingRecordsBySealed(@Param Boolean sealed);


}
