package org.commonjava.aprox.change.event;

import org.commonjava.aprox.io.StorageItem;

public class FileErrorEvent
    extends FileEvent
{
    private final Throwable error;

    public FileErrorEvent( final StorageItem storageLocation, final Throwable error )
    {
        super( storageLocation );
        this.error = error;
    }

    public Throwable getError()
    {
        return error;
    }

}
