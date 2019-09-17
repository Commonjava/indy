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
package org.commonjava.indy.subsys.prefetch;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.model.core.RemoteRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class PrefetchRepoComparator<T extends RemoteRepository>
        implements Comparator<RemoteRepository>
{
    @Override
    public int compare( RemoteRepository r1, RemoteRepository r2 )
    {
        if ( r1 == null )
        {
            return 1;
        }
        if ( r2 == null )
        {
            return -1;
        }

        final int priorityCompareResult = r2.getPrefetchPriority() - r1.getPrefetchPriority();
        if ( StringUtils.isBlank( r1.getPrefetchRescanTimestamp() ) && StringUtils.isBlank(
                r2.getPrefetchRescanTimestamp() ) )
        {
            return priorityCompareResult;
        }

        if ( StringUtils.isBlank( r1.getPrefetchRescanTimestamp() ) && StringUtils.isNotBlank(
                r2.getPrefetchRescanTimestamp() ) )
        {
            return -1;
        }
        else if ( StringUtils.isBlank( r2.getPrefetchRescanTimestamp() ) && StringUtils.isNotBlank(
                r1.getPrefetchRescanTimestamp() ) )
        {
            return 1;
        }

        final ZonedDateTime rescanTime1 =
                ZonedDateTime.parse( r1.getPrefetchRescanTimestamp(), RescanTimeUtils.UTC_TIME_FORMATTER );
        final ZonedDateTime rescanTime2 =
                ZonedDateTime.parse( r2.getPrefetchRescanTimestamp(), RescanTimeUtils.UTC_TIME_FORMATTER  );

        if ( rescanTime1.isBefore( rescanTime2 ) )
        {
            return -1;
        }
        else if ( rescanTime1.isAfter( rescanTime2 ) )
        {
            return 1;
        }
        else
        {
            return priorityCompareResult;
        }
    }

}
