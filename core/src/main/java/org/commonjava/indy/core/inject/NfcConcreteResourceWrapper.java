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
package org.commonjava.indy.core.inject;

import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

/**
 * Created by ruhan on 11/29/17.
 */
@Indexed
public class NfcConcreteResourceWrapper
{
    @Field( index = Index.YES, analyze = Analyze.NO )
    private String location;

    @Field ( index = Index.YES, analyze = Analyze.NO )
    private String path;

    @Field
    private long timeout;

    public NfcConcreteResourceWrapper( ConcreteResource resource, long timeout )
    {
        this.location = ( (KeyedLocation) resource.getLocation() ).getKey().toString();
        this.path = resource.getPath();
        this.timeout = timeout;
    }

    public String getLocation()
    {
        return location;
    }

    public String getPath()
    {
        return path;
    }

    public long getTimeout()
    {
        return timeout;
    }
}