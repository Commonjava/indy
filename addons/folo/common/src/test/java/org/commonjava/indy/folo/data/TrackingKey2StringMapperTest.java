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
package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.data.idxmodel.TrackingKey2StringMapper;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackingKey;
import org.infinispan.commons.marshall.WrappedByteArray;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class TrackingKey2StringMapperTest
{
    private static final String TEST_ID = "testid";

    @Test
    public void testMapper()
            throws IOException
    {
        TrackingKey2StringMapper mapper = new TrackingKey2StringMapper();
        TrackingKey key = new TrackingKey( TEST_ID );

        Assert.assertTrue( mapper.isSupportedType( TrackingKey.class ) );
        Assert.assertFalse( mapper.isSupportedType( Object.class ) );
        Assert.assertFalse( mapper.isSupportedType( TrackedContent.class ) );
        Assert.assertTrue( mapper.isSupportedType( WrappedByteArray.class ) );

        Object genKey = mapper.getKeyMapping( TEST_ID );
        Assert.assertEquals( key, genKey );

        String genId = mapper.getStringMapping( key );
        Assert.assertEquals( TEST_ID, genId );

        WrappedByteArray array = bytesOfTrackingKey( key );
        genId = mapper.getStringMapping( array );
        Assert.assertEquals( TEST_ID, genId );

    }

    private WrappedByteArray bytesOfTrackingKey( TrackingKey key )
            throws IOException
    {
        byte[] bytes = null;
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream stream = new ObjectOutputStream( byteOut ))
        {
            key.writeExternal( stream );
            bytes = byteOut.toByteArray();
            return new WrappedByteArray( bytes, Arrays.hashCode( bytes ) );
        }
    }
}
