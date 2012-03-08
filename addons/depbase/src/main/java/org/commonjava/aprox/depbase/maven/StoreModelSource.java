package org.commonjava.aprox.depbase.maven;

import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.model.building.ModelSource;
import org.commonjava.aprox.core.rest.StoreInputStream;

public class StoreModelSource
    implements ModelSource
{

    private final StoreInputStream stream;

    public StoreModelSource( final StoreInputStream stream )
    {
        this.stream = stream;
    }

    @Override
    public InputStream getInputStream()
        throws IOException
    {
        return stream;
    }

    @Override
    public String getLocation()
    {
        return stream.toString();
    }

}
