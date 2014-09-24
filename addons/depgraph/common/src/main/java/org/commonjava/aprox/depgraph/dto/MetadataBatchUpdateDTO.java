package org.commonjava.aprox.depgraph.dto;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class MetadataBatchUpdateDTO
    implements Iterable<Map.Entry<ProjectVersionRef, MetadataUpdateDTO>>
{

    private Map<ProjectVersionRef, MetadataUpdateDTO> updates;

    public MetadataBatchUpdateDTO( final Map<ProjectVersionRef, MetadataUpdateDTO> updates )
    {
        this.updates = updates;
    }

    public Map<ProjectVersionRef, MetadataUpdateDTO> getUpdates()
    {
        return updates;
    }

    public void setUpdates( final Map<ProjectVersionRef, MetadataUpdateDTO> updates )
    {
        this.updates = updates;
    }

    public boolean isEmpty()
    {
        return updates.isEmpty();
    }

    @Override
    public Iterator<Entry<ProjectVersionRef, MetadataUpdateDTO>> iterator()
    {
        return updates.entrySet()
                      .iterator();
    }

}
