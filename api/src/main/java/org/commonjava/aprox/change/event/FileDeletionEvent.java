package org.commonjava.aprox.change.event;

import org.commonjava.aprox.io.StorageItem;

public class FileDeletionEvent
    extends FileEvent
{
    public FileDeletionEvent( final StorageItem storageLocation )
    {
        super( storageLocation );
    }

}
