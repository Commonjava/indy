/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
