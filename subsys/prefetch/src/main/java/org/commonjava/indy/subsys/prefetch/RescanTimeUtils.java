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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RescanTimeUtils
{
    static final DateTimeFormatter UTC_TIME_FORMATTER =
            DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss 'UTC'Z" );

    static String getNextRescanTimeFromNow( final Integer intervalSeconds )
    {

        return ZonedDateTime.now( ZoneId.of( "UTC" ) ).plusSeconds( intervalSeconds ).format( UTC_TIME_FORMATTER );
    }

    static Boolean isNowAfter( final String rescanTime )
    {
        return ZonedDateTime.now( ZoneId.of( "UTC" ) ).isAfter( ZonedDateTime.parse( rescanTime, UTC_TIME_FORMATTER ) );
    }
}
