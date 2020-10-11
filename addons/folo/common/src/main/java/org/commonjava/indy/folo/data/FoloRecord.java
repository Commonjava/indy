package org.commonjava.indy.folo.data;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.o11yphant.metrics.annotation.Measure;

import java.util.Set;

public interface FoloRecord {
    @Measure
    boolean recordArtifact(TrackedContentEntry entry)
            throws FoloContentException, IndyWorkflowException;

    @Measure
    void delete(TrackingKey key);

    void replaceTrackingRecord(TrackedContent record);

    boolean hasRecord(TrackingKey key);

    boolean hasSealedRecord(TrackingKey key);

    @Measure
    boolean hasInProgressRecord(TrackingKey key);

    TrackedContent get(TrackingKey key);

    @Measure
    TrackedContent seal(TrackingKey trackingKey);

    Set<TrackingKey> getInProgressTrackingKey();

    Set<TrackingKey> getSealedTrackingKey();

    Set<TrackedContent> getSealed();

    void addSealedRecord(TrackedContent record);
}
