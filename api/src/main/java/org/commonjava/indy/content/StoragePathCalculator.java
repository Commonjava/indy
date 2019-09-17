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

import org.commonjava.indy.model.core.StoreKey;

/**
 * Support separation between logical path and storage path, usually for package metadata. This allows package-specific
 * path manipulations for how Indy stores content on the filesystem, without affecting the path used to transfer the
 * content.
 */
public interface StoragePathCalculator
{
    String calculateStoragePath( StoreKey storeKey, String path );
}
