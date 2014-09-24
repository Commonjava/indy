package org.commonjava.aprox.depgraph.dto;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MetadataUpdateDTO
    implements Iterable<Map.Entry<String, String>>
{

    private Map<String, String> updates;

    public MetadataUpdateDTO()
    {
    }

    public MetadataUpdateDTO( final Map<String, String> updates )
    {
        this.updates = updates;
    }

    public Map<String, String> getUpdates()
    {
        return updates;
    }

    public void setUpdates( final Map<String, String> updates )
    {
        this.updates = updates;
    }

    @Override
    public Iterator<Entry<String, String>> iterator()
    {
        return updates.entrySet()
                      .iterator();
    }

    public boolean isEmpty()
    {
        return updates.isEmpty();
    }

}
