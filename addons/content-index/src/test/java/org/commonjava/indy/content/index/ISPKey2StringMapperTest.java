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
package org.commonjava.indy.content.index;

import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;
import static org.junit.Assert.*;

public class ISPKey2StringMapperTest
{

    @Test
    public void testSerAndDeSerVersion1()
    {
        StoreKey key = StoreKey.fromString( "maven:remote:central" );
        IndexedStorePath isp = new IndexedStorePath( key, key, "/org/apache/maven/maven-metadata.xml" );

        ISPFieldStringKey2StringMapper mapper = new ISPFieldStringKey2StringMapper(  );

        assertTrue( mapper.isSupportedType( isp.getClass() ) );

        String l = mapper.getStringMapping( isp );

        Object o = mapper.getKeyMapping( l );

        assertTrue( o instanceof IndexedStorePath );

        assertEquals(isp,  o);
    }

    @Test
    public void testSerAndDeSerVersion2()
    {
        StoreKey key = StoreKey.fromString( "maven:remote:central" );
        IndexedStorePath isp = new IndexedStorePath( key, key, "/org/apache/maven/maven-metadata.xml" );

        ISPFieldStringKey2StringMapper mapper = new ISPFieldStringKey2StringMapper(  );

        assertTrue( mapper.isSupportedType( isp.getClass() ) );

        String l = mapper.getStringMapping( isp );

        Object o = mapper.getKeyMapping( l );

        assertTrue( o instanceof IndexedStorePath );

        assertEquals(isp,  o);
    }
}
