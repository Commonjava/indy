package org.commonjava.aprox.change.event;

import org.commonjava.aprox.io.StorageItem;
import org.commonjava.util.logging.Logger;

public class FileEvent
    implements AproxEvent
{

    private final StorageItem storageItem;

    protected FileEvent( final StorageItem storageItem )
    {
        this.storageItem = storageItem;
    }

    public StorageItem getStorageItem()
    {
        new Logger( getClass() ).info( "Retrieving file-event storage item: %s\n  from: %s", storageItem,
                                       new Throwable().getStackTrace()[1] );
        return storageItem;
    }

    public String getExtraInfo()
    {
        return "";
    }

    @Override
    public String toString()
    {
        return String.format( "%s [%s, storageItem=%s]", getClass().getSimpleName(), getExtraInfo(), storageItem );
    }

}