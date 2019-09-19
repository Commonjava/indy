/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.content;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;

import java.util.Set;

/**
 * Created by jdcasey on 1/20/17.
 *
 * Implemented to supplement the core aggregated-content management features of
 * {@link org.commonjava.indy.content.ContentGenerator} instances. This is preferable to @{@link javax.enterprise.event.Observes}
 * methods for {@link org.commonjava.maven.galley.event.FileDeletionEvent} or {@link org.commonjava.maven.galley.event.FileStorageEvent}
 * since it is more tightly coupled with the content generator's execution.
 */
public interface MergedContentAction
{
    void clearMergedPath( ArtifactStore originatingStore, Set<Group> affectedGroups, String path );
}
