/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.indy.content.StoreResource;

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
