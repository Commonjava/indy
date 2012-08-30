package org.commonjava.aprox.tensor.maven;

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.model.building.ModelSource;
import org.commonjava.aprox.core.io.StorageItem;

public class StoreModelSource
    implements ModelSource
{

    private final StorageItem item;

    public StoreModelSource( final StorageItem stream )
    {
        this.item = stream;
    }

    @Override
    public InputStream getInputStream()
        throws IOException
    {
        return item == null ? null : item.getStream();
    }

    @Override
    public String getLocation()
    {
        return item.toString();
    }

}
