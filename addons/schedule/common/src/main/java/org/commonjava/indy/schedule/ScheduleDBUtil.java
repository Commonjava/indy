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
package org.commonjava.indy.schedule;

public class ScheduleDBUtil
{

    public static final String TABLE_SCHEDULE = "schedule";
    public static final String TABLE_EXPIRATION = "expiration";

    public static String getSchemaCreateTableSchedule( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_SCHEDULE + " ("
                        + "jobtype varchar,"
                        + "jobname varchar,"
                        + "scheduleuid uuid,"
                        + "storekey varchar,"
                        + "payload varchar,"
                        + "scheduletime timestamp,"
                        + "lifespan bigint,"
                        + "expired boolean,"
                        + "PRIMARY KEY (storekey, jobname)"
                        + ");";
    }

    public static String getSchemaCreateTableExpiration( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_EXPIRATION + " ("
                        + "expirationpid bigint,"
                        + "scheduleuid uuid,"
                        + "expirationtime timestamp,"
                        + "storekey varchar,"
                        + "jobname varchar,"
                        + "PRIMARY KEY (expirationpid, storekey, jobname)"
                        + ");";
    }

    public static String getSchemaCreateTypeIndex4Schedule( String keyspace )
    {
        return "CREATE INDEX IF NOT EXISTS type_idx on " + keyspace + "." + TABLE_SCHEDULE + " (jobtype)";
    }

}
