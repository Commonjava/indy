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

//    boolean hasSealedRecord(TrackingKey key);

//    @Measure
//    boolean hasInProgressRecord(TrackingKey key);

    TrackedContent get(TrackingKey key);

    @Measure
    TrackedContent seal(TrackingKey trackingKey);

    Set<TrackingKey> getInProgressTrackingKey();

    Set<TrackingKey> getSealedTrackingKey();

    Set<TrackedContent> getSealed();

    void addSealedRecord(TrackedContent record);

    // To fix a Cassandra folo table issue, we abandon the old legacy table and use a new table. Ref to FoloRecordCassandra.
    default Set<TrackingKey> getLegacyTrackingKeys()
    {
        return null;
    }

    default TrackedContent getLegacy( TrackingKey tk )
    {
        return null;
    }
}
