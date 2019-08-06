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
