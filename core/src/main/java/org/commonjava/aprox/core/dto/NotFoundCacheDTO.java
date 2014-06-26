package org.commonjava.aprox.core.dto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.model.StoreKey;

public class NotFoundCacheDTO
{

    private final Set<NotFoundCacheSectionDTO> sections = new HashSet<NotFoundCacheSectionDTO>();

    public void addSection( final StoreKey key, final List<String> paths )
    {
        sections.add( new NotFoundCacheSectionDTO( key, paths ) );
    }

    public void addSection( final NotFoundCacheSectionDTO sectionDto )
    {
        sections.add( sectionDto );
    }

    public Set<NotFoundCacheSectionDTO> getSections()
    {
        return sections;
    }
}
