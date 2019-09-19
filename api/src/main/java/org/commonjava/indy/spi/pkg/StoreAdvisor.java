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
package org.commonjava.indy.spi.pkg;

import org.commonjava.indy.model.core.ArtifactStore;

/**
 */
public interface StoreAdvisor
{
    /**
     * Allow a packaging type to indicate that the proposed storage location is inappropriate for the supplied path
     *
     * @param path
     * @param store
     * @return
     */
    boolean vetoStorage( String path, ArtifactStore store );
}
