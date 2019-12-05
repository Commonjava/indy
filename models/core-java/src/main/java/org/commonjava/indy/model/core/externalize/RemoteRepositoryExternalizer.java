/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.model.core.externalize;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.infinispan.commons.marshall.Externalizer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RemoteRepositoryExternalizer
        implements Externalizer<RemoteRepository>
{
    private IndyObjectMapper objectMapper = new IndyObjectMapper( true );

    @Override
    public void writeObject( ObjectOutput output, RemoteRepository object )
            throws IOException
    {
        objectMapper.writeValue( output, object );
    }

    @Override
    public RemoteRepository readObject( ObjectInput input )
            throws IOException, ClassNotFoundException
    {
        return objectMapper.readValue( input, RemoteRepository.class );
    }
}
