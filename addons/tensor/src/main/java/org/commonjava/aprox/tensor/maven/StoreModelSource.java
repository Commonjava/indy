package org.commonjava.aprox.tensor.maven;

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.model.building.ModelSource;
import org.commonjava.aprox.io.StorageItem;

public class StoreModelSource
    implements ModelSource
{

    private final StorageItem item;

    private final boolean fireEvents;

    public StoreModelSource( final StorageItem stream, final boolean fireEvents )
    {
        this.item = stream;
        this.fireEvents = fireEvents;
    }

    @Override
    public InputStream getInputStream()
        throws IOException
    {
        return item == null ? null : item.openInputStream( fireEvents );
    }

    @Override
    public String getLocation()
    {
        return item.toString();
    }

}
