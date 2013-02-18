package org.commonjava.aprox.change.event;

import org.commonjava.aprox.io.StorageItem;

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