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
package org.commonjava.indy.filer.def.metrics;

import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.indy.metrics.system.StoragePathProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;

/**
 * Used to provide storage path in {@link org.commonjava.indy.metrics.system.SystemGaugesSet} to monitor
 * storage space usage
 */
@ApplicationScoped
public class DefaultStoragePathProvider
        implements StoragePathProvider
{
    @Inject
    private DefaultStorageProviderConfiguration storageConfig;

    @Override
    public File getStoragePath()
    {
        return storageConfig != null ? storageConfig.getStorageRootDirectory() : null;
    }
}
