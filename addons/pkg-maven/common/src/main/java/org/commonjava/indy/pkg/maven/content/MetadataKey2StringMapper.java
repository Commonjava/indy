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
package org.commonjava.indy.pkg.maven.content;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;

public final class MetadataKey2StringMapper
                implements TwoWayKey2StringMapper
{

    @Override
    public Object getKeyMapping( String s )
    {
        return MetadataKey.fromString( s );
    }

    @Override
    public boolean isSupportedType( Class<?> aClass )
    {
        return aClass == MetadataKey.class;
    }

    @Override
    public String getStringMapping( Object o )
    {
        return o.toString();
    }
}
