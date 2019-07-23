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
package org.commonjava.indy.pkg.npm.content.group;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;

public interface PackageMetadataProvider
{
    /**
     * Retrieve the list of versions available for the given NPM package, which should be included
     * in the target store.
     * @param targetStore This is the target where metadata aggregation is happening.
     * @param path path of the package.json that was requested. Useful to determine what kind of metadata to retrieve.
     * @return a {@link Metadata} instance containing the "virtual" versions that could become available.
     * @throws IndyWorkflowException
     */
    PackageMetadata getMetadata( StoreKey targetStore, String path ) throws IndyWorkflowException;
}
