package org.commonjava.aprox.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.aprox.content.StoreResource;

public final class ContentUtils
{

    private ContentUtils()
    {
    }

    /**
     * Attempt to remove duplicates, even "fuzzy" ones where a directory is 
     * listed with trailing '/' in some cases but not others.
     */
    public static List<StoreResource> dedupeListing( final List<StoreResource> listing )
    {
        final List<StoreResource> result = new ArrayList<StoreResource>();
        final Map<String, StoreResource> mapping = new LinkedHashMap<String, StoreResource>();
        for ( final StoreResource res : listing )
        {
            final String path = res.getPath();
            if ( mapping.containsKey( path ) )
            {
                continue;
            }

            if ( mapping.containsKey( path + "/" ) )
            {
                continue;
            }

            mapping.put( path, res );
            result.add( res );
        }

        return result;
    }

}
