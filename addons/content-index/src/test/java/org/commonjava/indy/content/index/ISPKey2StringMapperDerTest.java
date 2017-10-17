/**
 * Copyright (C) 2013 Red Hat, Inc.
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

import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ISPKey2StringMapperDerTest
{
    @Test
    @Ignore
    public void test()
            throws Exception
    {
        Class.forName( "org.postgresql.Driver" );
        try (Connection conn = DriverManager.getConnection( "jdbc:postgresql://localhost/indy", "indy", "indy" ))
        {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery( "select * from \"INDY_CACHE_content_index\"" );
            ISPKey2StringMapper2 mapper = new ISPKey2StringMapper2();
            Set<IndexedStorePath> s = new HashSet<>();
            List<IndexedStorePath> l = new ArrayList<>();
            while ( rs.next() )
            {
                String id = rs.getString( 1 );
                IndexedStorePath isp = (IndexedStorePath) mapper.getKeyMapping( id );
                s.add( isp );
                l.add( isp );
            }
            System.out.println( s.size() );
            System.out.println( l.size() );
            Map<IndexedStorePath, List<IndexedStorePath>> sorted = new HashMap<>();
            for ( IndexedStorePath i : l )
            {
                if ( sorted.containsKey( i ) )
                {
                    List<IndexedStorePath> contained = sorted.get( i );
                    contained.add( i );
                }
                else
                {
                    List<IndexedStorePath> contained = new ArrayList<>();
                    contained.add( i );
                    sorted.put( i, contained );
                }
            }

            for ( Map.Entry<IndexedStorePath, List<IndexedStorePath>> e : sorted.entrySet() )
            {
                if ( e.getValue().size() > 1 )
                {
                    System.out.println(e.getValue().size());
                }
            }

        }

    }
}
