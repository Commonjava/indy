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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by gli on 11/30/16.
 */
public class ScheduleManagerTest
{
    @Test
    public void testCalculateNextExpireTime()
            throws Exception
    {
        assertResult( 5000, System.currentTimeMillis() - 1000, 2000, false );
        assertResult( 10000, System.currentTimeMillis() - 3000, 3000, false );
        assertResult( 2000, System.currentTimeMillis() - 1000, 2000, true );
        assertResult( 2000, System.currentTimeMillis() - 3000, 0, true );
    }

    @Test
    public void testStoreKeyFrom()
    {
        StoreKey k = new StoreKey( "maven", StoreType.remote, "repo1" );
        ScheduleKey key = new ScheduleKey(k, ScheduleManager.CONTENT_JOB_TYPE, "/abc");
        final StoreKey sk = ScheduleManager.storeKeyFrom( key.groupName() );
        System.out.println(">>> " + sk);
        assertNotNull( sk );
        assertEquals( sk, k );
    }

    private void assertResult( final long expire, final long start, final long timeGone, final boolean nullable )
            throws Exception
    {
        final long nextTimeExpectInMillis = start + expire;
        final Instant startInInst = Instant.ofEpochMilli( start )
                                           .atZone( ZoneId.systemDefault() )
                                           .toLocalDateTime()
                                           .atZone( ZoneId.systemDefault() )
                                           .toInstant();
        if ( timeGone > 0 )
        {
            Thread.sleep( timeGone );
        }
        final Date nextTime = ScheduleManager.calculateNextExpireTime( expire, start );

        if ( nullable )
        {
            assertNull( nextTime );
        }
        else
        {
            assertNotNull( nextTime );
            assertTrue( nextTime.toInstant().isAfter( startInInst ) );
            assertTrue( Math.abs( nextTime.toInstant().toEpochMilli() - nextTimeExpectInMillis ) < 5 );
        }
    }
}
