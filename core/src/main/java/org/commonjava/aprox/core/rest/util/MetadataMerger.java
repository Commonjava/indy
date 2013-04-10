package org.commonjava.aprox.core.rest.util;

import java.util.Set;

import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.Group;

public interface MetadataMerger
{

    byte[] merge( final Set<StorageItem> sources, final Group group, final String path );

}
