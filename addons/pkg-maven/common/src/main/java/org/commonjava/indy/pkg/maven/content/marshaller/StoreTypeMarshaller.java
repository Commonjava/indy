/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.maven.content.marshaller;

import org.commonjava.indy.model.core.StoreType;
import org.infinispan.protostream.EnumMarshaller;

public class StoreTypeMarshaller implements EnumMarshaller<StoreType>
{

    @Override
    public StoreType decode( int enumValue )
    {
        if ( enumValue == 0 )
        {
            return StoreType.group;
        }
        else if ( enumValue == 1 )
        {
            return StoreType.remote;
        }
        else
        {
            return StoreType.hosted;
        }
    }

    @Override
    public int encode( StoreType storeType ) throws IllegalArgumentException
    {
        if ( storeType.equals( StoreType.group ) )
        {
            return 0;
        }
        else if ( storeType.equals( StoreType.remote ) )
        {
            return 1;
        }
        else
        {
            return 2;
        }
    }

    @Override
    public Class<? extends StoreType> getJavaClass()
    {
        return StoreType.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_key.StoreKey.StoreType";
    }


}
